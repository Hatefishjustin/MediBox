package com.medibox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Divider
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.ListRowVertical
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.Space3

@Composable
fun AppListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleMaxLines: Int = Int.MAX_VALUE,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val row = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = ScreenPaddingX, vertical = ListRowVertical)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space3)
        ) {
            if (leading != null) leading()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = AppText.BodyStrong,
                    color = Ink,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = AppText.Secondary,
                        color = InkSecondary,
                        maxLines = subtitleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (trailing != null) trailing()
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        row()
        if (showDivider) HorizontalDivider(color = Divider, thickness = 1.dp)
    }
}
