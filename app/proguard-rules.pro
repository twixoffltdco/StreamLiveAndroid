# StreamLive — правила R8/ProGuard для Release-сборки

# OkHttp 4 (Kotlin): стандартные рекомендации библиотеки
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# WebView: сохраняем классы, на которые ссылается JS-мост (если появится later)
-keep class android.webkit.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Стектрейсы читаемы в Crashlytics
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
