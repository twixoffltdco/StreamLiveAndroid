// Корневой build.gradle.kts — только плагины, версии зависимостей живут в app
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
