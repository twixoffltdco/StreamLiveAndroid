package app.platforma.streamlive

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/* ============================================================================
 *  ДОМЕНЫ STREAMLIVE (ВНИМАНИЕ: НЕ ПУТАТЬ!)
 *  PROD   — основной PHP-портал Platforma (стриминг, каналы, форум)
 *  RESERVE — резервный зеркала-домен, включается при падении основного
 * ==========================================================================*/
const val PROD_DOMAIN = "https://streamlivetv.freedev.app/"
const val RESERVE_DOMAIN = "https://domtv.blyz.ru/"

/** Активный источник контента после Failover-проверки. */
enum class ActiveSource { PROD, RESERVE }

/** Состояние оболочки StreamLive для нативного Compose-UI. */
sealed interface ConnectionState {
    /** Идёт HEAD-проверка доменов (Splash Screen держит экран). */
    data object Connecting : ConnectionState

    /** Основной сервер доступен — грузим PROD в WebView. */
    data class Ready(val url: String, val source: ActiveSource) : ConnectionState

    /** Резервный сервер доступен — грузим RESERVE + Snackbar о переключении. */
    data class ReadyReserve(val url: String) : ConnectionState

    /** Нет сети вообще — нативный экран «Нет интернета». */
    data object Offline : ConnectionState

    /** Сеть есть, но оба домена недоступны — «Технические работы». */
    data object ServersDown : ConnectionState
}

/**
 * NetworkMonitor — обёртка над ConnectivityManager на базе callbackFlow.
 * Эмитит true/false при каждом изменении сетевой связности.
 */
class NetworkMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val isOnlineNow: Boolean
        get() {
            val network = connectivityManager.activeNetwork ?: return false
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }

    /** Поток изменений подключения к интернету. */
    val networkStatus = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(false) }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(
                    networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                )
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
        // Первичное значение — текущее состояние сети
        trySend(isOnlineNow)

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}

/**
 * MainViewModel — «мозг» Failover-логики StreamLive.
 *
 * Алгоритм:
 *  1. Нет сети -> Offline (нативный экран с кнопкой «Повторить»).
 *  2. HEAD-запрос к PROD_DOMAIN с таймаутом 3 секунды.
 *     - HTTP 2xx (< 3 сек) -> Ready(PROD), грузим основной портал.
 *  3. Таймаут/ошибка PROD -> HEAD-запрос к RESERVE_DOMAIN.
 *     - OK -> ReadyReserve, грузим резерв + Snackbar «Подключен резервный сервер Platforma».
 *  4. Оба упали -> ServersDown (нативный экран «Технические работы»).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Connecting)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** Одноразовый сигнал для Snackbar о переключении на резерв. */
    private val _reserveNotice = MutableStateFlow(false)
    val reserveNotice: StateFlow<Boolean> = _reserveNotice.asStateFlow()

    private val networkMonitor = NetworkMonitor(application.applicationContext)

    // OkHttp: жёсткие таймауты, чтобы Failover решил судьбу домена за < 3 секунд
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(false) // решаем сами, без скрытых повторов
        .build()

    init {
        // Реагируем на «вызов сети из офлайна»: автоматически перепроверяем домены
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { online ->
                when (_state.value) {
                    ConnectionState.Offline ->
                        if (online) checkDomains() // сеть появилась — повторный Failover

                    ConnectionState.ServersDown ->
                        if (!online) _state.value = ConnectionState.Offline

                    else -> Unit // Ready / Connecting не трогаем
                }
            }
        }

        checkDomains()
    }

    /** Повторная проверка (кнопка «Повторить» на экранах-заглушках). */
    fun retry() {
        _state.value = ConnectionState.Connecting
        checkDomains()
    }

    /** Сообщили Snackbar'у о резерве, что пользователь его увидел. */
    fun consumeReserveNotice() {
        _reserveNotice.value = false
    }

    /**
     * Собственно Failover: PROD -> RESERVE -> ServersDown.
     * Используется и при старте, и при retry().
     */
    private fun checkDomains() {
        viewModelScope.launch(Dispatchers.IO) {
            // Шаг 0: быстрая локальная проверка связности
            if (!networkMonitor.isOnlineNow) {
                _state.value = ConnectionState.Offline
                return@launch
            }

            // Шаг 1: HEAD-запрос к основному домену
            if (headProbe(PROD_DOMAIN)) {
                _state.value = ConnectionState.Ready(PROD_DOMAIN, ActiveSource.PROD)
                return@launch
            }

            // Шаг 2: HEAD-запрос к резервному домену
            if (headProbe(RESERVE_DOMAIN)) {
                _reserveNotice.value = true
                _state.value = ConnectionState.ReadyReserve(RESERVE_DOMAIN)
                return@launch
            }

            // Шаг 3: оба упали — нативный экран «Технические работы»
            _state.value = if (networkMonitor.isOnlineNow) {
                ConnectionState.ServersDown
            } else {
                ConnectionState.Offline
            }
        }
    }

    /**
     * Лёгкий HEAD-запрос без тела ответа.
     * @return true только при HTTP 2xx (по условию — ответ успел прийти < 3 сек,
     *         это гарантирует callTimeout OkHttpClient).
     */
    private fun headProbe(url: String): Boolean = runCatching {
        val request = Request.Builder()
            .url(url)
            .head() // HEAD — не скачиваем HTML, экономим трафик пользователя
            .build()

        client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }.getOrDefault(false)

    override fun onCleared() {
        super.onCleared()
        // Освобождаем пул соединений OkHttp
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
