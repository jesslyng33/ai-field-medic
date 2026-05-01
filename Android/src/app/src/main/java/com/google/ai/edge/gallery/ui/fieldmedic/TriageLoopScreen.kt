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
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.HourglassTop
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.google.ai.edge.gallery.customtasks.fieldmedic.DetectionBox
import com.google.ai.edge.gallery.customtasks.fieldmedic.TriageIntent
import com.google.ai.edge.gallery.ui.theme.appFontFamily
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

private const val TAG = "TriageLoopScreen"

@Composable
fun TriageLoopScreen(
    viewModel: TriageLoopViewModel,
    onExit: () -> Unit,
    onEndSession: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current

    val currentPrompt by viewModel.currentPrompt.collectAsState()
    val userTranscript by viewModel.userTranscript.collectAsState()
    val conversationLog by viewModel.conversationLog.collectAsState()
    val vlmDescription by viewModel.vlmDescription.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isFlashOn by viewModel.isFlashOn.collectAsState()
    val loopState by viewModel.loopState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.ttsManager.isSpeaking.collectAsState()
    val frameGateStatus by viewModel.frameGateStatus.collectAsState()
    val detections by viewModel.latestDetections.collectAsState()
    val cameraFacing by viewModel.cameraFacing.collectAsState()
    val userIntent by viewModel.userIntent.collectAsState()
    val debugLog by viewModel.debugLog.collectAsState()
    var showDebug by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val activity = when {
        isSpeaking -> LoopActivity.SPEAKING
        isProcessing -> LoopActivity.PROCESSING
        isListening -> LoopActivity.LISTENING
        else -> LoopActivity.IDLE
    }

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
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    // Sync flash
    LaunchedEffect(isFlashOn, cameraControl) {
        cameraControl?.enableTorch(isFlashOn)
    }

    // Rebind camera whenever facing changes (after the first bind in the factory).
    LaunchedEffect(cameraFacing, cameraProvider, previewViewRef, permissionsGranted) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val pv = previewViewRef ?: return@LaunchedEffect
        if (!permissionsGranted) return@LaunchedEffect
        bindCamera(
            provider = provider,
            previewView = pv,
            lifecycleOwner = lifecycleOwner,
            imageCapture = imageCapture,
            facing = cameraFacing,
            onCameraControl = { cameraControl = it },
        )
    }

    // Capture a frame every time Gemma's TTS finishes speaking
    LaunchedEffect(permissionsGranted, loopState) {
        if (!permissionsGranted || loopState != LoopState.RUNNING) return@LaunchedEffect
        var wasSpeaking = false
        viewModel.ttsManager.isSpeaking.collect { speaking ->
            if (wasSpeaking && !speaking) {
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
            wasSpeaking = speaking
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
                    previewViewRef = previewView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProvider = provider
                        bindCamera(
                            provider = provider,
                            previewView = previewView,
                            lifecycleOwner = lifecycleOwner,
                            imageCapture = imageCapture,
                            facing = cameraFacing,
                            onCameraControl = { cameraControl = it },
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Layer 1.5: Detection annotation overlay (bounding boxes)
        if (detections.isNotEmpty()) {
            DetectionOverlay(
                detections = detections,
                mirrored = cameraFacing == CameraFacing.FRONT,
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
                onFlipCamera = { viewModel.toggleCameraFacing() },
                onSos = onExit,
                onEndSession = onEndSession,
            )

            // Activity status pill (LISTENING / SPEAKING / PROCESSING)
            ActivityPill(activity = activity, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.weight(1f))

            // Frame gate indicator + face count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                        text = if (frameGateStatus == true)
                            "FACE DETECTED (${detections.size})"
                        else "NO FACE",
                        color = if (frameGateStatus == true) FMGreenBright else FMRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = appFontFamily,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                }

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

            // Chat history bubbles
            ChatHistory(
                messages = conversationLog,
                livePartialTranscript = userTranscript,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(16.dp))

            // DIAGNOSIS / EMERGENCY mode toggle — switches the routing bias.
            // Selected button is bright; the other is dimmed.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val diagSelected = userIntent == TriageIntent.DIAGNOSIS
                val emergSelected = userIntent == TriageIntent.EMERGENCY

                androidx.compose.material3.FilledIconButton(
                    onClick = { viewModel.setUserIntent(TriageIntent.DIAGNOSIS) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (diagSelected) FMGreen else FMGreen.copy(alpha = 0.3f),
                        contentColor = FMText,
                    ),
                ) {
                    Text(
                        "DIAGNOSIS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                        letterSpacing = 2.sp,
                    )
                }

                androidx.compose.material3.FilledIconButton(
                    onClick = { viewModel.setUserIntent(TriageIntent.EMERGENCY) },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (emergSelected) FMRed else FMRed.copy(alpha = 0.3f),
                        contentColor = FMText,
                    ),
                ) {
                    Text(
                        "EMERGENCY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = appFontFamily,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectionOverlay(
    detections: List<DetectionBox>,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        detections.forEach { d ->
            val l = if (mirrored) (1f - d.right) else d.left
            val r = if (mirrored) (1f - d.left) else d.right
            val left = l * size.width
            val top = d.top * size.height
            val right = r * size.width
            val bottom = d.bottom * size.height
            drawRect(
                color = Color(0xFF43A047),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(width = 4f),
            )
        }
    }
}

@Composable
private fun ActivityPill(activity: LoopActivity, modifier: Modifier = Modifier) {
    if (activity == LoopActivity.IDLE) return
    val (text, color, icon) = when (activity) {
        LoopActivity.LISTENING -> Triple("LISTENING", FMGreenBright, Icons.Filled.Hearing)
        LoopActivity.SPEAKING -> Triple("SPEAKING", FMRed, Icons.Filled.GraphicEq)
        LoopActivity.PROCESSING -> Triple("THINKING", FMTextSub, Icons.Filled.HourglassTop)
        LoopActivity.IDLE -> return
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

@Composable
private fun ChatHistory(
    messages: List<TriageMessage>,
    livePartialTranscript: String,
    modifier: Modifier = Modifier,
) {
    // Filter SYSTEM messages out of the chat view
    val visible = remember(messages) {
        messages.filter { it.role != TriageRole.SYSTEM }
    }
    val listState = rememberLazyListState()

    // Auto-scroll to the latest message when the count changes
    LaunchedEffect(visible.size, livePartialTranscript) {
        val lastIndex = visible.size + (if (livePartialTranscript.isNotBlank()) 1 else 0) - 1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(visible) { msg ->
            ChatBubble(
                role = msg.role,
                text = formatMessageContent(msg.role, msg.content),
                isLive = false,
            )
        }
        if (livePartialTranscript.isNotBlank()) {
            item {
                ChatBubble(
                    role = TriageRole.USER,
                    text = livePartialTranscript,
                    isLive = true,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(
    role: TriageRole,
    text: String,
    isLive: Boolean,
) {
    val isUser = role == TriageRole.USER
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) FMGreen.copy(alpha = 0.85f)
                      else Color.Black.copy(alpha = 0.75f)
    val label = if (isUser) "You" else "Medic"

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = label,
                color = FMText.copy(alpha = 0.55f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = appFontFamily,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Box(
                modifier = Modifier
                    .background(bubbleColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = text,
                    color = FMText.copy(alpha = if (isLive) 0.65f else 1f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = appFontFamily,
                    fontStyle = if (isLive) androidx.compose.ui.text.font.FontStyle.Italic
                                else androidx.compose.ui.text.font.FontStyle.Normal,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

// Cleans up the raw log content for display in chat bubbles.
private fun formatMessageContent(role: TriageRole, content: String): String {
    if (role != TriageRole.USER) return content
    // "User said: \"foo\"" → "foo"
    val saidPrefix = "User said: \""
    if (content.startsWith(saidPrefix) && content.endsWith("\"")) {
        return content.removePrefix(saidPrefix).removeSuffix("\"")
    }
    // "User pressed YES" → "YES"
    val pressedPrefix = "User pressed "
    if (content.startsWith(pressedPrefix)) {
        return content.removePrefix(pressedPrefix)
    }
    return content
}

@Composable
private fun TopBar(
    isMuted: Boolean,
    isFlashOn: Boolean,
    onMuteToggle: () -> Unit,
    onFlashToggle: () -> Unit,
    onFlipCamera: () -> Unit,
    onSos: () -> Unit,
    onEndSession: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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

        Spacer(Modifier.width(8.dp))

        androidx.compose.material3.FilledIconButton(
            onClick = onEndSession,
            shape = RoundedCornerShape(14.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = FMText,
            ),
            modifier = Modifier
                .height(44.dp)
                .widthIn(min = 88.dp),
        ) {
            Text(
                "END",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = appFontFamily,
                letterSpacing = 2.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        // Camera flip
        IconButton(
            onClick = onFlipCamera,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.5f),
                contentColor = FMText,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(
                Icons.Filled.Cameraswitch,
                contentDescription = "Flip camera",
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(Modifier.width(8.dp))

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

private fun bindCamera(
    provider: ProcessCameraProvider,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    imageCapture: ImageCapture,
    facing: CameraFacing,
    onCameraControl: (androidx.camera.core.CameraControl) -> Unit,
) {
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
    val selector = if (facing == CameraFacing.FRONT)
        CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    try {
        provider.unbindAll()
        val camera = provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageCapture,
        )
        onCameraControl(camera.cameraControl)
    } catch (e: Exception) {
        Log.e(TAG, "Camera bind failed", e)
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
