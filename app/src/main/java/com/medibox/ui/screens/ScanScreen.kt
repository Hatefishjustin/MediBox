package com.medibox.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.medibox.domain.ocr.CandidateParser
import com.medibox.domain.ocr.OcrCandidate
import com.medibox.domain.ocr.OcrStatus
import com.medibox.domain.ocr.StabilityTracker
import com.medibox.ui.components.AppButtonGhost
import com.medibox.ui.components.AppButtonPrimary
import com.medibox.ui.components.AppButtonSecondary
import com.medibox.ui.components.AppTopBar
import com.medibox.ui.navigation.Routes
import com.medibox.ui.theme.AppText
import com.medibox.ui.theme.BorderStrong
import com.medibox.ui.theme.Ink
import com.medibox.ui.theme.InkSecondary
import com.medibox.ui.theme.InkTertiary
import com.medibox.ui.theme.Paper
import com.medibox.ui.theme.RadiusLg
import com.medibox.ui.theme.RadiusMd
import com.medibox.ui.theme.ScreenPaddingX
import com.medibox.ui.theme.Space2
import com.medibox.ui.theme.Space3
import com.medibox.ui.theme.Space4
import com.medibox.ui.theme.Surface
import com.medibox.ui.theme.SurfaceVariant
import java.util.concurrent.Executor

@Composable
fun ScanScreen(nav: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember(context) { LifecycleCameraController(context) }
    val recognizer = remember { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }

    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanStatus by remember { mutableStateOf(OcrStatus.PREPARING) }
    var statusText by remember { mutableStateOf("把药盒正面放进画面，尽量让文字清楚。") }
    var failureText by remember { mutableStateOf<String?>(null) }
    var latestCandidate by remember { mutableStateOf<OcrCandidate?>(null) }
    var navigated by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted) {
            scanStatus = OcrStatus.FAILED
            statusText = "需要相机权限才能扫描药盒。"
        }
    }

    val analyzer = remember(recognizer, mainExecutor) {
        OcrFrameAnalyzer(
            recognizer = recognizer,
            mainExecutor = mainExecutor,
            onStatus = { status, message ->
                scanStatus = status
                statusText = message
                if (status != OcrStatus.FAILED) {
                    failureText = null
                }
            },
            onFailure = { message ->
                scanStatus = OcrStatus.FAILED
                statusText = "没看清包装上的字。"
                failureText = message
            },
            onStableCandidate = {
                latestCandidate = it
            }
        )
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(cameraPermissionGranted) {
        if (cameraPermissionGranted) {
            cameraController.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.setImageAnalysisAnalyzer(mainExecutor, analyzer)
            hasFlashUnit = cameraController.cameraInfo?.hasFlashUnit() == true
            scanStatus = OcrStatus.ANALYZING
            statusText = "正在识别药盒上的文字…"
        } else {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
        }
    }

    LaunchedEffect(torchEnabled) {
        if (cameraPermissionGranted) {
            runCatching { cameraController.cameraControl?.enableTorch(torchEnabled) }
        }
    }

    LaunchedEffect(latestCandidate) {
        val candidate = latestCandidate ?: return@LaunchedEffect
        if (navigated) return@LaunchedEffect
        navigated = true
        cameraController.clearImageAnalysisAnalyzer()
        nav.navigate(buildOcrConfirmRoute(candidate)) {
            launchSingleTop = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
            cameraController.unbind()
            recognizer.close()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "扫描药盒",
                onBack = { nav.popBackStack() },
                actions = {
                    if (hasFlashUnit) {
                        IconButton(onClick = { torchEnabled = !torchEnabled }) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                                contentDescription = if (torchEnabled) "关闭闪光灯" else "打开闪光灯",
                                tint = InkSecondary
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Paper)
        ) {
            if (cameraPermissionGranted) {
                CameraPreview(
                    controller = cameraController,
                    modifier = Modifier.fillMaxSize()
                )
                ScanBottomSheet(
                    status = scanStatus,
                    statusText = statusText,
                    failureText = failureText,
                    onRetry = {
                        navigated = false
                        latestCandidate = null
                        analyzer.reset()
                        scanStatus = OcrStatus.ANALYZING
                        statusText = "正在重新识别药盒上的文字…"
                    },
                    onManualEntry = { nav.navigate(Routes.OCR_CONFIRM) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                )
            } else {
                PermissionGate(
                    onGrant = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onManualEntry = { nav.navigate(Routes.OCR_CONFIRM) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                this.controller = controller
                scaleType = PreviewView.ScaleType.FILL_CENTER
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        }
    )
}

@Composable
private fun ScanBottomSheet(
    status: OcrStatus,
    statusText: String,
    failureText: String?,
    onRetry: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(Surface, RadiusLg)
            .padding(horizontal = ScreenPaddingX, vertical = Space4)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space3)) {
            Text(
                text = when (status) {
                    OcrStatus.FAILED -> "没看清包装上的字。"
                    OcrStatus.SUCCESS -> "识别到了文字，正在整理结果…"
                    OcrStatus.ANALYZING -> "正在识别药盒上的文字…"
                    OcrStatus.PREPARING -> "准备扫描。"
                },
                style = AppText.TitleSmall,
                color = Ink
            )
            Text(
                text = statusText,
                style = AppText.Secondary,
                color = InkSecondary
            )
            if (failureText != null) {
                Text(
                    text = failureText,
                    style = AppText.Caption,
                    color = InkTertiary
                )
            }
            AppButtonSecondary(
                text = "重新识别",
                onClick = onRetry
            )
            AppButtonPrimary(
                text = "手动录入",
                onClick = onManualEntry
            )
        }
    }
}

@Composable
private fun PermissionGate(
    onGrant: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = ScreenPaddingX),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space4)
        ) {
            Text(
                text = "扫描药盒需要相机权限。",
                style = AppText.TitleSmall,
                color = Ink,
                textAlign = TextAlign.Center
            )
            Text(
                text = "允许权限后，直接把药盒正面放进画面即可。",
                style = AppText.Secondary,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
            AppButtonPrimary(text = "允许使用相机", onClick = onGrant)
            AppButtonGhost(text = "我先手动录入", onClick = onManualEntry)
        }
    }
}

private fun buildOcrConfirmRoute(candidate: OcrCandidate): String {
    fun String.encodeValue(): String = Uri.encode(this)
    fun bestValue(values: List<com.medibox.domain.ocr.OcrFieldCandidate>): String =
        values.firstOrNull()?.value.orEmpty().encodeValue()

    return Routes.OCR_CONFIRM_WITH_CANDIDATES
        .replace("{name}", bestValue(candidate.nameCandidates))
        .replace("{specification}", bestValue(candidate.specificationCandidates))
        .replace("{manufacturer}", bestValue(candidate.manufacturerCandidates))
        .replace("{batchNo}", bestValue(candidate.batchNoCandidates))
        .replace("{productionDate}", bestValue(candidate.productionDateCandidates))
        .replace("{expiryDate}", bestValue(candidate.expiryDateCandidates))
}

private class OcrFrameAnalyzer(
    private val recognizer: com.google.mlkit.vision.text.TextRecognizer,
    private val mainExecutor: Executor,
    private val onStatus: (OcrStatus, String) -> Unit,
    private val onFailure: (String) -> Unit,
    private val onStableCandidate: (OcrCandidate) -> Unit,
) : ImageAnalysis.Analyzer {

    private val tracker = StabilityTracker(requiredStableCount = 3, maxHistorySize = 5)
    private var inFlight = false
    private var lastProcessedAt = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val now = System.currentTimeMillis()
        if (inFlight || now - lastProcessedAt < 320L) {
            imageProxy.close()
            return
        }
        inFlight = true
        lastProcessedAt = now

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(inputImage)
            .addOnSuccessListener(mainExecutor) { result ->
                val candidate = CandidateParser.parse(result.text)
                if (candidate.hasAnyCandidate) {
                    onStatus(OcrStatus.ANALYZING, "正在识别药盒上的文字…")
                    val snapshot = tracker.push(candidate)
                    if (snapshot.isStable && snapshot.latestCandidate != null) {
                        onStatus(OcrStatus.SUCCESS, "识别结果已经稳定。")
                        onStableCandidate(snapshot.latestCandidate)
                    }
                } else {
                    onStatus(OcrStatus.ANALYZING, "正在识别药盒上的文字…")
                }
            }
            .addOnFailureListener(mainExecutor) { error ->
                onFailure(error.message ?: "识别失败，请重试。")
            }
            .addOnCompleteListener(mainExecutor) {
                inFlight = false
                imageProxy.close()
            }
    }

    fun reset() {
        tracker.reset()
        inFlight = false
        lastProcessedAt = 0L
    }
}
