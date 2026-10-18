package com.medibox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.BorderStrong
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.SurfaceSunken

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(label, style = AppText.Label, color = Ink, modifier = Modifier.padding(bottom = Space2))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceSunken, RadiusMd)
                .border(1.dp, BorderStrong, RadiusMd)
                .padding(horizontal = Space3, vertical = Space3),
            textStyle = AppText.Body.copy(color = Ink),
            singleLine = singleLine,
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            Text(placeholder, style = AppText.Body, color = InkTertiary)
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) trailingIcon()
                }
            }
        )
    }
}
