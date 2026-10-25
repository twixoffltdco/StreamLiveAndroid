package app.platforma.streamlive.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.platforma.streamlive.CookieManagerCompat
import app.platforma.streamlive.ConnectionState
import app.platforma.streamlive.MainViewModel
import kotlinx.coroutines.launch

/* ============================================================================
 *  AppShell — нативная оболочка StreamLive (Platforma)
 *  Scaffold + BottomNavigation («Главная», «О Платформе», «Профиль»)
 *  + WebView-контейнер с поддержкой JS, Fullscreen видео и Autoplay.
 * ==========================================================================*/

/** Вкладки нижней навигации. */
private enum class Tab(val label: String) {
    HOME("Главная"),
    ABOUT("О Платформе"),
    PROFILE("Профиль")
}

@Composable
fun AppShell(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reserveNotice by viewModel.reserveNotice.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(Tab.HOME.ordinal) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Snackbar «Подключен резервный сервер Platforma» при переключении на RESERVE
    LaunchedEffect(reserveNotice) {
        if (reserveNotice) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Подключен резервный сервер Platforma",
                    duration = SnackbarDuration.Long
                )
            }
            viewModel.consumeReserveNotice()
        }
    }

    Scaffold(
        // Чёрный фон под статус-баром и системной навигацией (edge-to-edge)
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StreamBottomBar(
                selected = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (val current = state) {
                // Сплэш уже держит этот экран — здесь показываем чёрный холст
                ConnectionState.Connecting -> Box(Modifier.fillMaxSize())

                ConnectionState.Offline ->
                    OfflineScreen(onRetry = viewModel::retry)

                ConnectionState.ServersDown ->
                    ServersDownScreen(
                        onRetry = viewModel::retry,
                        onAbout = { selectedTab = Tab.ABOUT.ordinal }
                    )

                is ConnectionState.Ready, is ConnectionState.ReadyReserve -> {
                    val url = when (current) {
                        is ConnectionState.Ready -> current.url
                        is ConnectionState.ReadyReserve -> current.url
                        else -> error("unreachable")
                    }

                    when (Tab.values()[selectedTab]) {
                        Tab.HOME -> StreamWebView(url = url)
                        Tab.ABOUT -> AboutPlatformScreen()
                        Tab.PROFILE -> ProfilePlaceholderScreen(
                            goHome = { selectedTab = Tab.HOME.ordinal }
                        )
                    }
                }
            }
        }
    }
}

/** Нижнее меню StreamLive: тёмное, с неоновой подсветкой активной вкладки. */
@Composable
private fun StreamBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = selected == Tab.HOME.ordinal,
            onClick = { onSelect(Tab.HOME.ordinal) },
            icon = { Icon(Icons.Rounded.Home, contentDescription = Tab.HOME.label) },
            label = { Text(Tab.HOME.label) },
            colors = navItemColors()
        )
        NavigationBarItem(
            selected = selected == Tab.ABOUT.ordinal,
            onClick = { onSelect(Tab.ABOUT.ordinal) },
            icon = { Icon(Icons.Rounded.Info, contentDescription = Tab.ABOUT.label) },
            label = { Text(Tab.ABOUT.label) },
            colors = navItemColors()
        )
        NavigationBarItem(
            selected = selected == Tab.PROFILE.ordinal,
            onClick = { onSelect(Tab.PROFILE.ordinal) },
            icon = {
                Icon(Icons.Rounded.PersonOutline, contentDescription = Tab.PROFILE.label)
            },
            label = { Text(Tab.PROFILE.label) },
            colors = navItemColors()
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.primary,   // неоновый циан
    selectedTextColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
)

/**
 * Нативный контейнер WebView для PHP-портала Platforma.
 *
 * Требования Store-Ready выполнены:
 *  - JavaScript включён;
 *  - Autoplay видео без жеста пользователя (медиа-политика);
 *  - Fullscreen видео через WebChromeClient.onShowCustomView;
 *  - Чёрный фон вместо белого (нет «белой вспышки» при загрузке);
 *  - Скроллбары скрыты — сайт выглядит как часть приложения;
 *  - Back-кнопка идёт по истории WebView;
 *  - Cookie сохраняются (сессии форума/личного кабинета).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StreamWebView(url: String) {
    val context = LocalContext.current

    // Полноэкранное видео: overlay поверх всей Activity
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    // Активность для PiP-хуков (VideoActivity-совместимое поведение)
    val activity = context as? Activity

    val webView = remember {
        WebView(context).apply {
            setBackgroundColor(AndroidColor.BLACK) // Убираем белый фон WebView

            settings.javaScriptEnabled = true      // Порталу нужен JS (плееры, форум)
            settings.domStorageEnabled = true      // localStorage сайта
            settings.databaseEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false // Autoplay стримов
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.loadsImagesAutomatically = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            // Third-party cookie — сторонним видео-плеерам портала нужна сессия Platforma
            CookieManagerCompat.enableThirdParty(this)

            // Автоскрытие скроллбаров — сайт должен выглядеть нативно
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // Все http(s)-ссылки грузим внутри оболочки;
                    // внешние схемы (mailto:, tg://, intent://) отдаём системе.
                    val uri = request?.url ?: return false
                    val scheme = uri.scheme ?: return false
                    return if (scheme == "http" || scheme == "https") {
                        false // оставляем в WebView
                    } else {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, uri
                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        true
                    }
                }

                override fun onPageFinished(view: WebView?, rawUrl: String?) {
                    // Инъекция стиля: скрываем полосу прокрутки сайта и красим body в чёрный,
                    // чтобы страница «растворялась» в интерфейсе приложения
                    view?.evaluateJavascript(
                        """
                        (function() {
                          var s = document.createElement('style');
                          s.textContent = 'html{background:#000!important;}::-webkit-scrollbar{display:none!important;}';
                          document.head && document.head.appendChild(s);
                        })();
                        """.trimIndent(), null
                    )
                }
            }

            webChromeClient = object : WebChromeClient() {
                // Fullscreen видео (кнопка «развернуть» в HTML5-плеерах портала)
                override fun onShowCustomView(view: View, callback: CustomViewCallback?) {
                    if (fullscreenView != null) {
                        callback?.onCustomViewHidden()
                        return
                    }
                    fullscreenView = view
                    view.visibility = View.VISIBLE
                    activity?.addContentView(
                        view,
                        android.widget.FrameLayout.LayoutParams(
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    visibility = View.GONE
                }

                override fun onHideCustomView() {
                    fullscreenView?.let { fv ->
                        activity?.window?.decorView?.removeView(fv)
                    }
                    fullscreenView = null
                    visibility = View.VISIBLE
                }

                // Прогресс загрузки можно показать линейным индикатором при необходимости
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                }
            }
        }
    }

    // Загрузка актуального URL (PROD или RESERVE после Failover)
    LaunchedEffect(url) {
        if (webView.url != url) webView.loadUrl(url)
    }

    // Освобождаем ресурсы WebView при выходе из композиции
    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }

    // Аппаратная «Назад»: сначала история браузера, потом выход
    BackHandler(enabled = fullscreenView == null) {
        if (webView.canGoBack()) webView.goBack()
    }
    BackHandler(enabled = fullscreenView != null) {
        webView.webChromeClient?.onHideCustomView()
    }

    AndroidView(
        factory = { webView },
        modifier = Modifier.fillMaxSize()
    )
}
