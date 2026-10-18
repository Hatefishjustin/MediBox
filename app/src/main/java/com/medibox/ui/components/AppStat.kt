package com.medibox.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.Space1

@Composable
fun AppStat(
    number: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Space1),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = number,
            style = AppText.Display.copy(color = Ink),
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = AppText.Caption.copy(color = InkTertiary),
            textAlign = TextAlign.Center
        )
    }
}
