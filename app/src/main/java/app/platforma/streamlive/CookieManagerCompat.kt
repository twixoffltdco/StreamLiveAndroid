package app.platforma.streamlive

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

/**
 * Утилита синхронизации cookie WebView.
 * Гарантирует, что сессии форума и личного кабинета Platforma
 * переживают перезапуск приложения (нужно для «Профиля»).
 */
object CookieManagerCompat {
    fun sync(context: Context) {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            flush()
        }
    }

    /** Полная очистка данных сайта (например, по кнопке «Выйти» в будущем). */
    /** Разрешаем third-party cookies в конкретном WebView (нужно для плееров портала). */
    fun enableThirdParty(webView: WebView) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
    }

    fun wipeAll(context: Context) {
        CookieManager.getInstance().removeAllCookies { flushed ->
            if (flushed) WebStorage.getInstance().deleteAllData()
        }
    }
}
