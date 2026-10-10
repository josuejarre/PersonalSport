package com.example.personalsport.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val ProGymDarkColorScheme = darkColorScheme(
    primary = ElectricSportOrange,
    onPrimary = PureBlack,
    primaryContainer = DarkOrangeTint,
    onPrimaryContainer = PureWhite,
    secondary = PlatinumSteel,
    onSecondary = PureWhite,
    secondaryContainer = DarkSteelAccent,
    onSecondaryContainer = PureWhite,
    tertiary = ElectricOrangeBright,
    onTertiary = PureBlack,
    background = CarbonDark,
    onBackground = TextPrimaryWhite,
    surface = DarkMetallicCard,
    onSurface = TextPrimaryWhite,
    surfaceVariant = DarkSteelAccent,
    onSurfaceVariant = LightPlatinum,
    surfaceContainerHigh = DarkSteelAccent,
    surfaceContainerHighest = MetallicGray,
    outline = MetallicGray,
    outlineVariant = MetallicBorder
)

@Composable
fun PersonalSportTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> ProGymDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
