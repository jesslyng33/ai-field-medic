package com.google.ai.edge.gallery.ui.fieldmedic

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private const val TAG = "TriageLoopScreen"

@Composable
fun TriageLoopScreen(
    viewModel: TriageLoopViewModel,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val currentPrompt by viewModel.currentPrompt.collectAsState()
    val vlmDescription by viewModel.vlmDescription.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val loopState by viewModel.loopState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val frameGateStatus by viewModel.frameGateStatus.collectAsState()

    // Keep screen on
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    // Permissions
    var permissionsGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        permissionsGranted = grants.values.all { it }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    // Camera setup
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }

    // Sync flash
    LaunchedEffect(isFlashOn, cameraControl) {
        cameraControl?.enableTorch(isFlashOn)
    }

    // Periodic frame capture
    LaunchedEffect(permissionsGranted, loopState) {
        if (!permissionsGranted || loopState != LoopState.RUNNING) return@LaunchedEffect
        while (true) {
            delay(FRAME_INTERVAL_MS)
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        val bitmap = imageProxyToBitmap(imageProxy)
                        imageProxy.close()
                        if (bitmap != null) {
                            viewModel.onFrameCaptured(bitmap)
                        }
                    }
                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Frame capture failed", exception)
                    }
                },
            )
        }
    }

    // Start loop after permissions
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) {
            viewModel.startLoop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { viewModel.toggleMute() })
            },
    ) {
        // Layer 1: Camera preview (full screen)
        if (permissionsGranted) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        try {
                            provider.unbindAll()
                            val camera = provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                            )
                            cameraControl = camera.cameraControl
                        } catch (e: Exception) {
                            Log.e(TAG, "Camera bind failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Layer 2: UI overlays
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Top bar
            TopBar(
                isMuted = isMuted,
                isFlashOn = isFlashOn,
                onMuteToggle = { viewModel.toggleMute() },
                onFlashToggle = { viewModel.toggleFlash() },
                onSos = onExit,
            )

            Spacer(Modifier.weight(1f))

            // Frame gate indicator + VLM description pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Gate status dot
                if (frameGateStatus != null) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (frameGateStatus == true) FMGreenBright else FMRed,
                                CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (frameGateStatus == true) "PASS" else "DROP",
                        color = if (frameGateStatus == true) FMGreenBright else FMRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = appFontFamily,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                }

                // VLM description
                if (vlmDescription.isNotBlank()) {
                    Text(
                        text = vlmDescription,
                        color = FMText,
                        fontSize = 13.sp,
                        fontFamily = appFontFamily,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.6f),
                                RoundedCornerShape(20.dp),
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Current prompt (main instruction)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = currentPrompt,
                    color = FMText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = appFontFamily,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Processing indicator
            if (isProcessing) {
                Text(
                    "Processing...",
                    color = FMTextSub,
                    fontSize = 12.sp,
                    fontFamily = appFontFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
            }

            // YES / NO buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // YES
                androidx.compose.material3.FilledIconButton(
                    onClick = { viewModel.onYesPressed() },
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = FMGreen,
                        contentColor = FMText,
                    ),
                ) {
                    Text(
                        "YES",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                        letterSpacing = 3.sp,
                    )
                }

                // NO
                androidx.compose.material3.FilledIconButton(
                    onClick = { viewModel.onNoPressed() },
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = FMRed,
                        contentColor = FMText,
                    ),
                ) {
                    Text(
                        "NO",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                        letterSpacing = 3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    isMuted: Boolean,
    isFlashOn: Boolean,
    onMuteToggle: () -> Unit,
    onFlashToggle: () -> Unit,
    onSos: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // SOS button
        IconButton(
            onClick = onSos,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = FMRed,
                contentColor = FMText,
            ),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "SOS",
                modifier = Modifier.size(28.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // Mute toggle
        IconButton(
            onClick = onMuteToggle,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = FMText,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isMuted) "Unmute" else "Mute",
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

        // Flash toggle
        IconButton(
            onClick = onFlashToggle,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = FMText,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                if (isFlashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = if (isFlashOn) "Flash Off" else "Flash On",
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: return null

    val rotation = imageProxy.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap

    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private const val FRAME_INTERVAL_MS = 5000L
