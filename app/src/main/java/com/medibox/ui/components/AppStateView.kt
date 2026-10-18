package com.medibox.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.Danger
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.Ok
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.Space8
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Medication

@Composable
fun AppEmptyState(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Medication, contentDescription = null, tint = InkTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(Space4))
        Text(title, style = AppText.TitleSmall, color = Ink)
        if (subtitle != null) {
            Spacer(Modifier.height(Space2))
            Text(subtitle, style = AppText.Secondary, color = InkSecondary, textAlign = TextAlign.Center)
        }
        if (action != null) {
            Spacer(Modifier.height(Space4))
            action()
        }
    }
}

@Composable
fun AppLoadingState(modifier: Modifier = Modifier, text: String? = null) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = InkSecondary, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        if (text != null) {
            Spacer(Modifier.height(Space2))
            Text(text, style = AppText.Secondary, color = InkSecondary)
        }
    }
}

@Composable
fun AppErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Danger, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Space4))
        Text(message, style = AppText.Secondary, color = InkSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space4))
        AppButtonPrimary("重试", onRetry, Modifier.width(160.dp))
    }
}

@Composable
fun AppSuccessState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = Ok, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Space2))
        Text(message, style = AppText.Secondary, color = InkSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
fun AppPermissionState(
    message: String,
    buttonText: String,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.CameraAlt, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Space4))
        Text(message, style = AppText.Secondary, color = InkSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space4))
        AppButtonPrimary(buttonText, onGrant, Modifier.width(200.dp))
    }
}

@Composable
fun AppOcrFailedState(onManual: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Space8),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.DocumentScanner, contentDescription = null, tint = InkSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(Space4))
        Text("没看清包装上的字", style = AppText.TitleSmall, color = Ink)
        Spacer(Modifier.height(Space2))
        Text("你可以直接手动填写药品信息。", style = AppText.Secondary, color = InkSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Space4))
        AppButtonPrimary("手动录入", onManual, Modifier.width(200.dp))
    }
}
