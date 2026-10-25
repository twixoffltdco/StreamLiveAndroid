package app.platforma.streamlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.platforma.streamlive.ui.theme.NeonCyan
import app.platforma.streamlive.ui.theme.NeonPink
import app.platforma.streamlive.ui.theme.StreamSurfaceVariant

/* ============================================================================
 *  Нативные Compose-экраны StreamLive: оффлайн, ошибки серверов, «О Платформе»
 *  Никаких HTML-заглушек — только нативный UI в фирменном стиле бренда.
 * ==========================================================================*/

/** Центральный «статусный» экран с иконкой в неоновом круге, заголовком и кнопками. */
@Composable
private fun StatusScreen(
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Иконка в светящемся круге — фирменный приём стриминговых приложений
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(StreamSurfaceVariant)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onPrimary,
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = MaterialTheme.colorScheme.background
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(primaryLabel, fontWeight = FontWeight.Bold)
        }

        if (secondaryLabel != null && onSecondary != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSecondary,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(secondaryLabel)
            }
        }
    }
}

/** Экран «Нет интернета» — показывается, когда NetworkMonitor не видит сеть. */
@Composable
fun OfflineScreen(onRetry: () -> Unit) {
    StatusScreen(
        icon = Icons.Rounded.WifiOff,
        accent = NeonCyan,
        title = "Нет интернета",
        subtitle = "Похоже, ваше устройство не подключено к сети.\n" +
                "Проверьте Wi-Fi или мобильные данные и попробуйте снова.",
        primaryLabel = "Повторить",
        onPrimary = onRetry
    )
}

/** Экран «Серверы недоступны» / «Технические работы» — оба домена упали. */
@Composable
fun ServersDownScreen(onRetry: () -> Unit, onAbout: () -> Unit) {
    StatusScreen(
        icon = Icons.Rounded.CloudOff,
        accent = NeonPink,
        title = "Серверы StreamLive временно недоступны",
        subtitle = "На портале Platforma идут технические работы.\n" +
                "Мы уже чиним — попробуйте подключиться через несколько минут.",
        primaryLabel = "Повторить попытку",
        onPrimary = onRetry,
        secondaryLabel = "О Платформе",
        onSecondary = onAbout
    )
}

/** Строка «иконка + текст» для информационного списка на экране About. */
@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Статичный экран «О Платформе» — брендовая витрина Platforma / StreamLive. */
@Composable
fun AboutPlatformScreen(onRetry: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Логотип-заглушка: буква «S» в неоновом круге
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        listOf(NeonCyan, NeonPink)
                    )
                )
        ) {
            Text(
                text = "S",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.background
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "StreamLive",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Продукт материнской платформы Platforma",
            style = MaterialTheme.typography.bodyMedium,
            color = NeonPink,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "StreamLive — это ваш прямой эфир с платформой: тысячи телеканалов, " +
                    "стримы, сериалы и живое общение на форуме — всё в одном лёгком " +
                    "нативном приложении.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 26.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            "Что внутри",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        InfoRow(Icons.Rounded.Home, "Прямые эфиры и телеканалы в HD-качестве")
        InfoRow(Icons.Rounded.PersonOutline, "Личный профиль и подписки Platforma")
        InfoRow(Icons.Rounded.Settings, "Умный резервный сервер при сбоях основного")
        InfoRow(Icons.Rounded.Info, "Форум и сообщество — прямо внутри приложения")

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            "Версия приложения 1.0.0\n© Platforma, 2026. Все права защищены.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Экран «Профиль»: портал требует авторизации на стороне PHP-сайта.
 * Даём нативный вход — переключаемся на вкладку «Главная», где пользователь
 * авторизуется в личном кабинете Platforma внутри WebView.
 */
@Composable
fun ProfilePlaceholderScreen(goHome: () -> Unit) {
    StatusScreen(
        icon = Icons.Rounded.PersonOutline,
        accent = NeonCyan,
        title = "Профиль Platforma",
        subtitle = "Войдите в личный кабинет на главной странице —\n" +
                "подписки, история просмотров и форум синхронизируются автоматически.",
        primaryLabel = "Перейти на главную",
        onPrimary = goHome
    )
}
