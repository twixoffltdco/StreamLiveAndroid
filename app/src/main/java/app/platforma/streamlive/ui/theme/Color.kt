package app.platforma.streamlive.ui.theme

import androidx.compose.ui.graphics.Color

/* ============================================================================
 *  Палитра StreamLive (Platforma) — тёмный стриминговый стиль
 *  Чёрный фон + неоновые акценты портала
 * ==========================================================================*/

// Фирменные цвета бренда
val StreamBlack = Color(0xFF000000)        // Основной фон — чистый чёрный (OLED-friendly)
val StreamSurface = Color(0xFF0B0B0F)      // Поверхности карточек / BottomBar
val StreamSurfaceVariant = Color(0xFF16161C) // Вариант поверхности (чипы, поля)
val StreamOutline = Color(0xFF2A2A33)      // Тонкие разделители

// Акценты платформы (как на портале)
val NeonCyan = Color(0xFF00F2EA)           // Primary-акцент — бирюзовый неон
val NeonPink = Color(0xFFFE2C55)           // Secondary-акцент — «живой» розовый (LIVE)
val NeonCyanDark = Color(0xFF00B8B2)       // Dark-вариант циана для контраста
val LiveRed = Color(0xFFFF1E4D)            // Индикатор прямых эфиров

// Текст
val TextPrimary = Color(0xFFF2F2F7)        // Основной текст на чёрном
val TextSecondary = Color(0xFF9A9AA5)      // Вторичный/подписи

// Статусные
val ErrorRed = Color(0xFFCF6679)
val SnackbarBackground = Color(0xFF1E1E26)
