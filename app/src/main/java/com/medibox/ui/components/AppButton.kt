package com.medibox.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.BorderStrong
import com.medibox.ui.theme.Brand
import com.medibox.ui.theme.BrandSoft
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkDisabled
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.Surface

@Composable
fun AppButtonPrimary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RadiusMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand,
            contentColor = Color.White,
            disabledContainerColor = InkDisabled.copy(alpha = 0.25f),
            disabledContentColor = Surface
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(text, style = AppText.Label, color = LocalContentColor.current)
    }
}

@Composable
fun AppButtonSecondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RadiusMd,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Ink,
            disabledContentColor = InkDisabled
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderStrong)
        ),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(text, style = AppText.Label, color = LocalContentColor.current)
    }
}

@Composable
fun AppButtonGhost(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.padding(Space2),
        colors = ButtonDefaults.textButtonColors(contentColor = Brand)
    ) {
        Text(text, style = AppText.Label)
    }
}
