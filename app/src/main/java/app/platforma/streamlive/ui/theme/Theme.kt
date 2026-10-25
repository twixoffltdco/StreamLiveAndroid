package app.platforma.streamlive.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/* ============================================================================
 *  StreamLive Theme — всегда тёмная тема в стиле портала Platforma.
 *  Stripping: чёрный фон, неоновый циан #00F2EA, живой розовый #FE2C55.
 * ==========================================================================*/

private val StreamLiveColorScheme = darkColorScheme(
    primary = NeonCyan,               // Кнопки, активные иконки навигации
    onPrimary = StreamBlack,          // Тёмный текст на неоне — максимальный контраст
    primaryContainer = StreamSurfaceVariant,
    onPrimaryContainer = TextPrimary,

    secondary = NeonPink,             // Акценты «LIVE», бейджи
    onSecondary = TextPrimary,
    secondaryContainer = StreamSurfaceVariant,
    onSecondaryContainer = NeonPink,

    tertiary = LiveRed,
    onTertiary = TextPrimary,

    background = StreamBlack,         // Фон приложения — чистый чёрный
    onBackground = TextPrimary,

    surface = StreamSurface,          // BottomNavigation, карточки
    onSurface = TextPrimary,
    surfaceVariant = StreamSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    outline = StreamOutline,
    error = ErrorRed,
    onError = StreamBlack
)

/** Типографика в духе видеоплатформ: жирные заголовки, компактные подписи. */
private val StreamLiveTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        labelSmall = base.labelSmall.copy(letterSpacing = 0.5.sp)
    )
}

/** Углы Material3: крупные скругления в духе мобильных видеоплатформ. */
object StreamShapes {
    val card: Shape = RoundedCornerShape(16.dp)
    val button: Shape = RoundedCornerShape(24.dp)
    val sheet: Shape = RectangleShape
}

/**
 * Корневая тема StreamLive.
 * Приложение Store-Ready: принудительно тёмная тема (как у стриминговых брендов),
 * статус-бар затемнён, светлые системные иконки.
 */
@Composable
fun StreamLiveTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val activity = LocalContext.current as? Activity

    SideEffect {
        // Статус-бар прозрачный + белые иконки (edge-to-edge уже включён в MainActivity)
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = StreamLiveColorScheme,
        typography = StreamLiveTypography,
        content = content
    )
}
