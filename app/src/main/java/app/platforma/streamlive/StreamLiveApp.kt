package app.platforma.streamlive

import android.app.Application

/**
 * Application-класс StreamLive.
 * Здесь можно инициализировать аналитику/Firebase/Crashlytics перед релизом.
 * Пока — чистая оболочка без лишних зависимостей (требование Google Play Data Safety).
 */
class StreamLiveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Разрешаем WebView сохранять историю/cookies между сессиями (личный кабинет Platforma)
        CookieManagerCompat.sync(this)
    }
}
