package com.medibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Brand
import com.medibox.ui.theme.BrandSoft
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.RadiusPill
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.SurfaceVariant

@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(if (selected) BrandSoft else SurfaceVariant, RadiusPill)
            .border(
                if (selected) 1.dp else 0.dp,
                if (selected) Brand else Color.Transparent,
                RadiusPill
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Space3 + Space2, vertical = Space2),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppText.Label,
            color = if (selected) Brand else InkSecondary,
            textAlign = TextAlign.Center
        )
    }
}
