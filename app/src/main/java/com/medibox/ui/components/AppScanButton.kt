package com.medibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.Brand
import com.medibox.ui.theme.OnBrand
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner

@Composable
fun AppScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Brand)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = "扫描药品",
            tint = OnBrand,
            modifier = Modifier.size(26.dp)
        )
    }
}
