package app.platforma.streamlive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import app.platforma.streamlive.ui.AppShell
import app.platforma.streamlive.ui.theme.StreamLiveTheme

/**
 * StreamLive (материнский бренд — Platforma / Платформа)
 *
 * Точка входа нативной оболочки для PHP-портала (стриминг, каналы, форум).
 * Здесь устанавливается официальный AndroidX Splash Screen API: система
 * показывает SplashScreen до тех пор, пока ViewModel не завершит проверку
 * основного и резервного доменов (KeepOnScreenCondition).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Устанавливаем Splash Screen ДО setContent — обязательное требование API
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Edge-to-edge: WebView занимает всё пространство, включая статус-бар
        enableEdgeToEdge()

        // 2. Создаём ViewModel до Compose, чтобы подписка на состояние была живой
        val mainViewModel: MainViewModel = viewModel()

        // 3. Держим системный сплэш, пока идёт HEAD-проверка доменов.
        //    Как только состояние выходит из CONNECTING — сплэш исчезает
        //    с анимацией и появляется нативный UI.
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.state.value is ConnectionState.Connecting
        }

        setContent {
            StreamLiveTheme {
                AppShell(viewModel = mainViewModel)
            }
        }
    }

    /**
     * PiP (Picture-in-Picture): разрешаем «картинку в картинке» для видеоконтента.
     * Вызывается из WebChromeClient при запросе полноэкранного видео.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Хук оставлен пустым: вход в PiP инициируется оболочкой AppShell,
        // когда WebView активен и играет видео (см. enterPictureInPictureMode ниже).
    }

    fun enterPip() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                android.app.PictureInPictureParams.Builder().build()
            )
        }
    }

}
