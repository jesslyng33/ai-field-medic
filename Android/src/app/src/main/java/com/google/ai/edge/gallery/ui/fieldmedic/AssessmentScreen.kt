package com.google.ai.edge.gallery.ui.fieldmedic

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import java.util.concurrent.Executors

@Composable
fun AssessmentScreen(
    viewModel: FieldMedicViewModel,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val audioRecorded by viewModel.audioRecorded.collectAsState()
    val photoBitmap by viewModel.photoBitmap.collectAsState()
    var isRecording by remember { mutableStateOf(false) }
    var photoTaken by remember { mutableStateOf(photoBitmap != null) }
    var notes by remember { mutableStateOf("") }

    // Camera state
    var showCamera by remember { mutableStateOf(false) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Permission launchers
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isRecording = true
            viewModel.startRecording()
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    val hasInput = audioRecorded || photoTaken || notes.isNotBlank()

    val pulse = rememberInfiniteTransition(label = "micPulse")
    val micRingScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "micRingScale",
    )
    val micRingAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "micRingAlpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FMBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = FMTextSub)
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "WHAT\nHAPPENED?",
            color = FMText,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            fontFamily = appFontFamily,
            lineHeight = 46.sp,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Use voice, photo, or type — use whatever is easiest right now.",
            color = FMTextSub,
            fontSize = 15.sp,
            fontFamily = appFontFamily,
            lineHeight = 22.sp,
        )

        Spacer(Modifier.height(40.dp))

        // Mic button
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Pulse ring when recording
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .graphicsLayer {
                                scaleX = micRingScale
                                scaleY = micRingScale
                                alpha = micRingAlpha
                            }
                            .background(FMRed.copy(alpha = 0.3f), CircleShape),
                    )
                }
                // Main mic button
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = if (isRecording) FMRed else FMSurface,
                            shape = CircleShape,
                        )
                        .border(
                            width = 2.dp,
                            color = if (audioRecorded) FMGreenBright else if (isRecording) FMRed else FMBorder,
                            shape = CircleShape,
                        )
                        .clickable {
                            if (isRecording) {
                                isRecording = false
                                viewModel.stopRecording()
                            } else {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    isRecording = true
                                    viewModel.startRecording()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording",
                        tint = if (isRecording) FMText else if (audioRecorded) FMGreenBright else FMTextSub,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    isRecording -> "Tap to stop"
                    audioRecorded -> "Audio captured"
                    else -> "Hold to record"
                },
                color = when {
                    isRecording -> FMRed
                    audioRecorded -> FMGreenBright
                    else -> FMTextSub
                },
                fontSize = 13.sp,
                fontFamily = appFontFamily,
                letterSpacing = 0.5.sp,
            )
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = FMDivider)
        Spacer(Modifier.height(24.dp))

        // Camera + Notes row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Camera button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(FMSurface, RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (photoTaken) FMGreenBright else FMBorder,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            if (!photoTaken) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    showCamera = true
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            } else {
                                photoTaken = false
                                showCamera = false
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Take photo",
                            tint = if (photoTaken) FMGreenBright else FMTextSub,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            if (photoTaken) "Photo added" else "Add photo",
                            color = if (photoTaken) FMGreenBright else FMTextSub,
                            fontSize = 13.sp,
                            fontFamily = appFontFamily,
                        )
                    }
                }
            }
        }

        // Camera preview
        if (showCamera && !photoTaken) {
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(FMSurface, RoundedCornerShape(12.dp)),
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()
                            imageCapture = capture
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                capture,
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.matchParentSize(),
                )
            }
            Spacer(Modifier.height(8.dp))
            FMPrimaryButton(
                text = "CAPTURE",
                onClick = {
                    val capture = imageCapture ?: return@FMPrimaryButton
                    capture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(imageProxy: ImageProxy) {
                            val bmp = imageProxyToBitmap(imageProxy)
                            imageProxy.close()
                            if (bmp != null) {
                                viewModel.setPhoto(bmp)
                                photoTaken = true
                                showCamera = false
                                // Unbind camera
                                try {
                                    ProcessCameraProvider.getInstance(context).get().unbindAll()
                                } catch (_: Exception) {}
                            }
                        }
                        override fun onError(e: ImageCaptureException) {
                            android.util.Log.e("AssessmentScreen", "Capture failed", e)
                        }
                    })
                },
                color = FMRed,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Notes field
        FMSectionLabel("OR TYPE WHAT HAPPENED")
        Spacer(Modifier.height(8.dp))
        FMTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = "Describe injury, pain level, symptoms...",
            singleLine = false,
            minLines = 3,
            maxLines = 6,
        )

        Spacer(Modifier.weight(1f))

        // Analyze button
        FMPrimaryButton(
            text = "ANALYZE",
            onClick = {
                viewModel.setNotes(notes)
                onAnalyze()
            },
            color = if (hasInput) FMRed else FMBorder,
        )
        Spacer(Modifier.height(8.dp))
        if (!hasInput) {
            Text(
                "Add voice, photo, or notes above to continue",
                color = FMTextSub,
                fontSize = 12.sp,
                fontFamily = appFontFamily,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val rotation = imageProxy.imageInfo.rotationDegrees
    if (rotation == 0) return bitmap
    val matrix = Matrix()
    matrix.postRotate(rotation.toFloat())
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
