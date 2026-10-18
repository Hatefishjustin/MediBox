package com.medibox.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Brand, onPrimary = OnBrand,
    primaryContainer = BrandSoft, onPrimaryContainer = BrandInk,
    secondary = InkSecondary, onSecondary = Surface,
    secondaryContainer = SurfaceVariant, onSecondaryContainer = Ink,
    tertiary = Brand, onTertiary = OnBrand,
    tertiaryContainer = BrandSoft, onTertiaryContainer = BrandInk,
    error = Danger, onError = OnBrand,
    errorContainer = DangerSoft, onErrorContainer = Danger,
    background = Paper, onBackground = Ink,
    surface = Surface, onSurface = Ink,
    surfaceVariant = SurfaceVariant, onSurfaceVariant = InkSecondary,
    outline = BorderStrong, outlineVariant = Divider,
    scrim = Scrim,
    inverseSurface = Ink, inverseOnSurface = Paper, inversePrimary = BrandSoft
)

private val AppShapes = Shapes(
    extraSmall = RadiusXs,
    small = RadiusSm,
    medium = RadiusMd,
    large = RadiusLg,
    extraLarge = RadiusLg
)

@Composable
fun MediBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // S0 ships Light only; Dark is evaluated in S9.
    val colorScheme = LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppType,
        shapes = AppShapes,
        content = content
    )
}
