package com.medibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Brand
import com.medibox.ui.theme.BrandSoft
import com.medibox.ui.theme.Danger
import com.medibox.ui.theme.DangerSoft
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.RadiusSm
import com.medibox.ui.theme.Space1
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.SurfaceVariant
import com.medibox.ui.theme.Warning
import com.medibox.ui.theme.WarningSoft

enum class TagType { Neutral, Warning, Danger, Brand }

@Composable
fun AppTag(
    text: String,
    type: TagType = TagType.Neutral,
    modifier: Modifier = Modifier,
) {
    val (bg: Color, fg: Color) = when (type) {
        TagType.Neutral -> SurfaceVariant to InkSecondary
        TagType.Warning -> WarningSoft to Warning
        TagType.Danger -> DangerSoft to Danger
        TagType.Brand -> BrandSoft to Brand
    }
    Box(
        modifier = modifier
            .background(bg, RadiusSm)
            .padding(horizontal = Space2, vertical = Space1)
    ) {
        Text(
            text = text,
            style = AppText.Caption.copy(color = fg, fontWeight = FontWeight.Medium)
        )
    }
}
