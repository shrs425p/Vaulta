package com.vaulta.viewer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vaulta.storage.index.VaultEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    entryId: String,
    onNavigateBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(entryId) {
        viewModel.loadFile(entryId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentEntry: VaultEntry? = when (val state = uiState) {
        is ViewerUiState.ImageReady -> state.entry
        is ViewerUiState.VideoReady -> state.entry
        is ViewerUiState.AudioReady -> state.entry
        is ViewerUiState.PdfReady -> state.entry
        is ViewerUiState.CsvReady -> state.entry
        is ViewerUiState.TextReady -> state.entry
        is ViewerUiState.DocumentReady -> state.entry
        is ViewerUiState.BinaryReady -> state.entry
        else -> null
    }

    DisposableEffect(entryId) {
        onDispose {
            viewModel.cleanup()
        }
    }

    val isVideoReady = uiState is ViewerUiState.VideoReady

    Scaffold(
        topBar = {
            if (!isVideoReady) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentEntry?.name ?: "Secure Viewer",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isVideoReady) Modifier else Modifier.padding(padding))
        ) {
            when (val state = uiState) {
                is ViewerUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Decrypting file in secure memory...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ViewerUiState.ImageReady -> {
                    ZoomableImageViewer(
                        bitmap = state.bitmap,
                        contentDescription = state.entry.name
                    )
                }

                is ViewerUiState.VideoReady -> {
                    VideoPlayer(
                        dataSourceFactory = state.dataSourceFactory,
                        entry = state.entry,
                        onNavigateBack = onNavigateBack,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ViewerUiState.AudioReady -> {
                    AudioPlayer(
                        dataSourceFactory = state.dataSourceFactory,
                        entry = state.entry,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ViewerUiState.PdfReady -> {
                    PdfViewer(
                        entry = state.entry,
                        pdfFile = state.pdfFile,
                        pageCount = state.pageCount,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ViewerUiState.CsvReady -> {
                    CsvViewer(
                        state = state,
                        onCopySensitiveText = { viewModel.copySensitiveText("CSV", it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ViewerUiState.DocumentReady -> {
                    DocumentView(
                        state = state,
                        onCopySensitiveText = { viewModel.copySensitiveText("Document", it) }
                    )
                }

                is ViewerUiState.TextReady -> {
                    TextViewer(
                        state = state,
                        onCopySensitiveText = { viewModel.copySensitiveText("Text", it) },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is ViewerUiState.BinaryReady -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Size: ${formatFileSize(state.file.length())}\nMIME: ${state.entry.mimeType ?: "binary/octet-stream"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is ViewerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadFile(entryId) }) {
                            Text("Retry Decryption")
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun ZoomableImageViewer(
    bitmap: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        offset += pan
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}

@Composable
private fun VideoPlayer(
    dataSourceFactory: EncryptedMediaDataSource.Factory,
    entry: VaultEntry,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var isControlsVisible by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var resizeModeIndex by rememberSaveable { mutableIntStateOf(0) }

    val resizeModes = remember {
        listOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
            AspectRatioFrameLayout.RESIZE_MODE_FILL
        )
    }
    val resizeLabels = remember {
        listOf("Fit (Letterbox)", "Zoom (Fill Screen)", "Stretch")
    }

    val shouldBeImmersive = isFullscreen || isLandscape

    // Manage system bars immersion when fullscreen or landscape
    DisposableEffect(shouldBeImmersive) {
        val window = activity?.window
        if (window != null && shouldBeImmersive) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            onDispose {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        } else {
            onDispose {}
        }
    }

    // Reset orientation on exiting screen
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun toggleFullscreen() {
        val newFullscreen = !isFullscreen
        isFullscreen = newFullscreen
        if (newFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val exoPlayer = remember(dataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse("vaulta://encrypted-media"))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("VaultaViewer", "Secure playback failed")
                playbackError = error.message ?: "Playback error"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                    useController = true
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    resizeMode = resizeModes[resizeModeIndex]
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShowSubtitleButton(true)
                    setShowFastForwardButton(true)
                    setShowRewindButton(true)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    controllerShowTimeoutMs = 3500

                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                        isControlsVisible = (visibility == android.view.View.VISIBLE)
                    })

                    setFullscreenButtonClickListener {
                        toggleFullscreen()
                    }
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeModes[resizeModeIndex]
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay top controls (fades in/out in sync with video player controls)
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = entry.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )
                // Aspect Ratio toggle (Fit / Zoom / Stretch)
                IconButton(
                    onClick = {
                        resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                        Toast.makeText(context, "Aspect: ${resizeLabels[resizeModeIndex]}", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { toggleFullscreen() }) {
                    Icon(
                        if (shouldBeImmersive) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = "Fullscreen",
                        tint = Color.White
                    )
                }
            }
        }

        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hardware codec or format error: $playbackError",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "This file could not be decoded by the secure in-app player. External playback is disabled to keep plaintext inside Vaulta.",
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioPlayer(
    dataSourceFactory: EncryptedMediaDataSource.Factory,
    entry: VaultEntry,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    val exoPlayer = remember(dataSourceFactory) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse("vaulta://encrypted-media"))
            setMediaItem(mediaItem)
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    duration = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosition = exoPlayer.currentPosition.coerceAtLeast(0L)
            duration = exoPlayer.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.size(160.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AudioFile,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = entry.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = formatFileSize(entry.sizeBytes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Slider(
            value = if (duration > 0) (currentPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { ratio ->
                val seekPos = (ratio * duration).toLong()
                currentPosition = seekPos
                exoPlayer.seekTo(seekPos)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newPos = (exoPlayer.currentPosition - 10000).coerceAtLeast(0)
                exoPlayer.seekTo(newPos)
            }) {
                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", modifier = Modifier.size(32.dp))
            }

            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = {
                val newPos = (exoPlayer.currentPosition + 10000).coerceAtMost(duration)
                exoPlayer.seekTo(newPos)
            }) {
                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun PdfViewer(
    entry: VaultEntry,
    pdfFile: File,
    pageCount: Int,
    viewModel: ViewerViewModel,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("PDF document has no readable pages")
        }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

    var rotation by remember { mutableIntStateOf(0) }
    var isNightMode by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var jumpPageInput by remember { mutableStateOf("") }
    var documentScale by remember { mutableFloatStateOf(1f) }
    var isSinglePageView by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()

    val currentPage = if (isSinglePageView) {
        pagerState.currentPage + 1
    } else {
        (lazyListState.firstVisibleItemIndex + 1).coerceIn(1, pageCount)
    }

    val invertColorMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    if (showJumpDialog) {
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column {
                    Text("Enter page number (1 to $pageCount):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jumpPageInput,
                        onValueChange = { jumpPageInput = it.filter { ch -> ch.isDigit() } },
                        placeholder = { Text("e.g. 5") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val page = jumpPageInput.toIntOrNull()
                    if (page != null && page in 1..pageCount) {
                        coroutineScope.launch {
                            if (isSinglePageView) {
                                pagerState.animateScrollToPage(page - 1)
                            } else {
                                lazyListState.animateScrollToItem(page - 1)
                            }
                        }
                    }
                    showJumpDialog = false
                    jumpPageInput = ""
                }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // PDF Unified Tools Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Badge (tap to jump)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { showJumpDialog = true }
                ) {
                    Text(
                        text = "Page $currentPage / $pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Global Document Zoom Controller: [-] [ 100% ] [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newScale = (documentScale - 0.25f).coerceAtLeast(1.0f)
                            documentScale = newScale
                            if (newScale <= 1.05f) {
                                coroutineScope.launch { horizontalScrollState.scrollTo(0) }
                            }
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = documentScale > 1.0f
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Zoom Out All Pages",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (documentScale > 1.05f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable {
                            val newScale = if (documentScale > 1.05f) 1.0f else 2.0f
                            documentScale = newScale
                            if (newScale <= 1.05f) {
                                coroutineScope.launch { horizontalScrollState.scrollTo(0) }
                            }
                        }
                    ) {
                        Text(
                            text = "${(documentScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (documentScale > 1.05f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val newScale = (documentScale + 0.25f).coerceAtMost(4.0f)
                            documentScale = newScale
                        },
                        modifier = Modifier.size(32.dp),
                        enabled = documentScale < 4.0f
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Zoom In All Pages",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Action Tools: View Mode Toggle, Rotate, Night Mode, Print
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isSinglePageView = !isSinglePageView },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isSinglePageView) Icons.Default.ViewAgenda else Icons.Default.Layers,
                            contentDescription = if (isSinglePageView) "Continuous Scroll Mode" else "Single Page Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { rotation = (rotation + 90) % 360 },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = "Rotate 90°",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { isNightMode = !isNightMode },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Night Reading Mode",
                            tint = if (isNightMode) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (isSinglePageView) {
            // Single-page mode with page flip and slider
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

                    LaunchedEffect(pageIndex) {
                        withContext(Dispatchers.IO) {
                            bitmap = viewModel.renderPdfPage(pageIndex)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNightMode) Color(0xFF1E1E1E) else Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val pageBitmap = bitmap
                                if (pageBitmap != null) {
                                    val baseAspect = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
                                    val aspectRatio = if (rotation % 180 != 0) 1f / baseAspect else baseAspect
                                    Image(
                                        bitmap = pageBitmap.asImageBitmap(),
                                        contentDescription = "Page ${pageIndex + 1}",
                                        colorFilter = if (isNightMode) ColorFilter.colorMatrix(invertColorMatrix) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(aspectRatio)
                                            .graphicsLayer {
                                                scaleX = documentScale
                                                scaleY = documentScale
                                                rotationZ = rotation.toFloat()
                                            },
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }

                // Single Page Navigation Footer: [<] Slider [>]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Page")
                    }

                    Slider(
                        value = (pagerState.currentPage + 1).toFloat(),
                        onValueChange = { value ->
                            val target = value.roundToInt() - 1
                            if (target in 0 until pageCount) {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(target)
                                }
                            }
                        },
                        valueRange = 1f..pageCount.toFloat(),
                        steps = if (pageCount > 2) pageCount - 2 else 0,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < pageCount - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < pageCount - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Page")
                    }
                }
            }
        } else {
            // Continuous Vertical Scroll Mode with UNIFIED DOCUMENT ZOOM
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val containerWidth = maxWidth
                val targetWidth = containerWidth * documentScale

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    val newScale = if (documentScale > 1.2f) 1.0f else 2.0f
                                    documentScale = newScale
                                    if (newScale <= 1.05f) {
                                        coroutineScope.launch { horizontalScrollState.scrollTo(0) }
                                    }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    if (activePointers.size >= 2) {
                                        val zoom = event.calculateZoom()
                                        if (zoom != 1f) {
                                            val newScale = (documentScale * zoom).coerceIn(1f, 4f)
                                            documentScale = newScale
                                            if (newScale <= 1.05f) {
                                                coroutineScope.launch { horizontalScrollState.scrollTo(0) }
                                            }
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .horizontalScroll(
                            state = horizontalScrollState,
                            enabled = documentScale > 1.05f
                        )
                ) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .width(targetWidth)
                            .fillMaxHeight()
                            .padding(horizontal = if (documentScale <= 1.05f) 16.dp else 4.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(count = pageCount, key = { it }) { pageIndex ->
                            var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(pageIndex) {
                                withContext(Dispatchers.IO) {
                                    bitmap = viewModel.renderPdfPage(pageIndex)
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isNightMode) Color(0xFF1E1E1E) else Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Page ${pageIndex + 1} of $pageCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isNightMode) Color.LightGray else Color.DarkGray,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    val pageBitmap = bitmap
                                    if (pageBitmap != null) {
                                        val baseAspect = pageBitmap.width.toFloat() / pageBitmap.height.toFloat()
                                        val aspectRatio = if (rotation % 180 != 0) 1f / baseAspect else baseAspect
                                        Image(
                                            bitmap = pageBitmap.asImageBitmap(),
                                            contentDescription = "Page ${pageIndex + 1}",
                                            colorFilter = if (isNightMode) ColorFilter.colorMatrix(invertColorMatrix) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(aspectRatio)
                                                .graphicsLayer {
                                                    rotationZ = rotation.toFloat()
                                                },
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CsvViewer(
    state: ViewerUiState.CsvReady,
    onCopySensitiveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val filteredRows = remember(searchQuery, state.rows) {
        if (searchQuery.isBlank()) {
            state.rows
        } else {
            val q = searchQuery.lowercase()
            state.rows.filterIndexed { index, row ->
                index == 0 || row.any { it.lowercase().contains(q) }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Table View (${state.rows.size} rows)") },
                icon = { Icon(Icons.Default.TableChart, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Raw Data") },
                icon = { Icon(Icons.Default.ViewHeadline, contentDescription = null) }
            )
        }

        // Live Filter / Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(if (selectedTab == 0) "Filter table rows..." else "Search in raw data...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        if (selectedTab == 0) {
            if (searchQuery.isNotBlank()) {
                Text(
                    text = "Showing ${filteredRows.size} of ${state.rows.size} rows",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = filteredRows) { row ->
                        Row(
                            modifier = Modifier
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(vertical = 2.dp)
                        ) {
                            row.forEach { cell ->
                                Text(
                                    text = if (searchQuery.isNotBlank()) highlightText(cell, searchQuery) else AnnotatedString(cell),
                                    modifier = Modifier
                                        .width(135.dp)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = {
                        onCopySensitiveText(state.rawText)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Raw")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) highlightText(state.rawText, searchQuery) else AnnotatedString(state.rawText),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

    }
}

@Composable
private fun DocumentView(
    state: ViewerUiState.DocumentReady,
    onCopySensitiveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.slides.isNotEmpty()) {
        PresentationViewer(
            state = state,
            onCopySensitiveText = onCopySensitiveText,
            modifier = modifier
        )
    } else {
        WordDocumentViewer(
            state = state,
            onCopySensitiveText = onCopySensitiveText,
            modifier = modifier
        )
    }
}

@Composable
private fun PresentationViewer(
    state: ViewerUiState.DocumentReady,
    onCopySensitiveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    var showSlideshow by remember { mutableStateOf(false) }
    var slideshowStartPage by remember { mutableIntStateOf(0) }

    val filteredSlides = remember(searchQuery, state.slides) {
        if (searchQuery.isBlank()) {
            state.slides
        } else {
            val q = searchQuery.lowercase()
            state.slides.filter { slide ->
                slide.rawText.lowercase().contains(q) ||
                        (slide.title?.lowercase()?.contains(q) == true) ||
                        (slide.subtitle?.lowercase()?.contains(q) == true)
            }
        }
    }

    if (showSlideshow && state.slides.isNotEmpty()) {
        val pagerState = rememberPagerState(
            initialPage = slideshowStartPage.coerceIn(0, state.slides.size - 1),
            pageCount = { state.slides.size }
        )
        val coroutineScope = rememberCoroutineScope()

        Dialog(
            onDismissRequest = { showSlideshow = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val slide = state.slides[pageIndex]
                    val bgCol = remember(slide.backgroundColorHex) {
                        parseSlideColor(slide.backgroundColorHex, Color(0xFF1E293B))
                    }
                    val isLightBg = isColorLight(bgCol)
                    val textCol = if (isLightBg) Color(0xFF0F172A) else Color.White
                    val secTextCol = if (isLightBg) Color(0xFF475569) else Color(0xFFCBD5E1)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = bgCol)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Slide Text Content Column
                                Column(
                                    modifier = Modifier
                                        .weight(if (slide.images.isNotEmpty()) 0.62f else 1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "SLIDE ${slide.slideNumber} of ${state.slides.size}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        if (!slide.title.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = slide.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = textCol,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (!slide.subtitle.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = slide.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = secTextCol,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (slide.bullets.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                slide.bullets.take(5).forEach { bullet ->
                                                    Row(verticalAlignment = Alignment.Top) {
                                                        Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                        Text(bullet, style = MaterialTheme.typography.bodySmall, color = secTextCol, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Slide Visuals / Images Column
                                if (slide.images.isNotEmpty()) {
                                    val bmp = remember(slide.slideNumber) {
                                        try {
                                            BitmapFactory.decodeByteArray(slide.images.first(), 0, slide.images.first().size)
                                        } catch (_: Exception) { null }
                                    }
                                    if (bmp != null) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.35f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .sizeIn(maxWidth = 140.dp, maxHeight = 140.dp)
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(
                                                        if (isLightBg) Color.Black.copy(alpha = 0.06f)
                                                        else Color.White.copy(alpha = 0.08f)
                                                    )
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Image(
                                                    bitmap = bmp.asImageBitmap(),
                                                    contentDescription = "Slide Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Slideshow Floating Bottom Control Bar
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Slide", tint = Color.White)
                        }

                        Text(
                            text = "${pagerState.currentPage + 1} / ${state.slides.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (pagerState.currentPage < state.slides.size - 1) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                }
                            },
                            enabled = pagerState.currentPage < state.slides.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Slide", tint = Color.White)
                        }

                        IconButton(
                            onClick = { showSlideshow = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Slideshow", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Presentation Header Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Slideshow,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${state.slides.size} Slides",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search slides",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            slideshowStartPage = 0
                            showSlideshow = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Slideshow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Search text across all slides...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            }
        }

        if (isSearchActive && searchQuery.isNotBlank()) {
            Text(
                text = "Showing ${filteredSlides.size} of ${state.slides.size} matching slides",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Slide Deck Cards (16:9 widescreen presentation slides!)
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items = filteredSlides) { slide ->
                val bgCol = remember(slide.backgroundColorHex) {
                    parseSlideColor(slide.backgroundColorHex, Color(0xFF1E293B))
                }
                val isLightBg = isColorLight(bgCol)
                val textCol = if (isLightBg) Color(0xFF0F172A) else Color.White
                val secTextCol = if (isLightBg) Color(0xFF475569) else Color(0xFFCBD5E1)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = bgCol)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left / Content
                        Column(
                            modifier = Modifier
                                .weight(if (slide.images.isNotEmpty()) 0.62f else 1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "SLIDE ${slide.slideNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                slideshowStartPage = slide.slideNumber - 1
                                                showSlideshow = true
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Fullscreen, contentDescription = "Present Slide", tint = textCol, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                onCopySensitiveText(slide.rawText)
                                                Toast.makeText(context, "Slide ${slide.slideNumber} text copied", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = textCol, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (!slide.title.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) highlightText(slide.title, searchQuery) else AnnotatedString(slide.title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textCol,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!slide.subtitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) highlightText(slide.subtitle, searchQuery) else AnnotatedString(slide.subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = secTextCol,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (slide.bullets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        slide.bullets.take(4).forEach { bullet ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text("• ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                Text(
                                                    text = if (searchQuery.isNotBlank()) highlightText(bullet, searchQuery) else AnnotatedString(bullet),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.5.sp,
                                                    color = secTextCol,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Right / Embedded Slide Images
                        if (slide.images.isNotEmpty()) {
                            val bmp = remember(slide.slideNumber) {
                                try {
                                    BitmapFactory.decodeByteArray(slide.images.first(), 0, slide.images.first().size)
                                } catch (_: Exception) { null }
                            }
                            if (bmp != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.35f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .sizeIn(maxWidth = 96.dp, maxHeight = 96.dp)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isLightBg) Color.Black.copy(alpha = 0.06f)
                                                else Color.White.copy(alpha = 0.08f)
                                            )
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Slide Image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordDocumentViewer(
    state: ViewerUiState.DocumentReady,
    onCopySensitiveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var fontSizeSp by remember { mutableIntStateOf(13) } // Default 13sp: crisp, readable, not over-zoomed!
    var readingThemeIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var zoomPercent by remember { mutableIntStateOf(100) }

    val themes = listOf(
        Pair(Color.White, Color(0xFF1E293B)),           // Light (Clean White Paper)
        Pair(Color(0xFFFBF0D9), Color(0xFF3B2F2F)),     // Sepia (Warm Paper)
        Pair(Color(0xFF1E1E1E), Color(0xFFE4E4E4))      // Dark (OLED Midnight)
    )
    val themeLabels = listOf("Light", "Sepia", "Dark")
    val (bgColor, textColor) = themes[readingThemeIndex]
    val deskBg = if (readingThemeIndex == 2) Color(0xFF121212) else Color(0xFFF1F5F9)

    val wordCount = remember(state.previewText) {
        state.previewText?.split(Regex("""\s+"""))?.filter { it.isNotBlank() }?.size ?: 0
    }
    val charCount = state.previewText?.length ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(deskBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Document Reading & Zoom Controls: Font size (A- / A+), Zoom, Theme, Search, Copy
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Font Size Adjuster: A- (down to 10sp) / A+ (up to 22sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { if (fontSizeSp > 10) fontSizeSp -= 1 },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = "${fontSizeSp}sp",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    FilledTonalButton(
                        onClick = { if (fontSizeSp < 22) fontSizeSp += 1 },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Zoom: [-] 100% [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = { if (zoomPercent > 75) zoomPercent -= 15 },
                        modifier = Modifier.size(32.dp),
                        enabled = zoomPercent > 75
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(16.dp))
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.clickable { zoomPercent = if (zoomPercent != 100) 100 else 125 }
                    ) {
                        Text(
                            text = "$zoomPercent%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { if (zoomPercent < 200) zoomPercent += 15 },
                        modifier = Modifier.size(32.dp),
                        enabled = zoomPercent < 200
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(16.dp))
                    }
                }

                // Theme, Search & Copy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            readingThemeIndex = (readingThemeIndex + 1) % themes.size
                            Toast.makeText(context, "Theme: ${themeLabels[readingThemeIndex]}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.FormatColorFill, contentDescription = "Theme", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { isSearchActive = !isSearchActive },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Find in text",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            onCopySensitiveText(state.previewText.orEmpty())
                            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180))
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Find in document text...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Simulated A4/Letter Paper Sheet Container
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth((zoomPercent / 100f).coerceIn(0.75f, 1f))
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    val docx = state.docxDoc
                    if (docx != null && docx.paragraphs.isNotEmpty()) {
                        // Render Document Title
                        if (!docx.title.isNullOrBlank()) {
                            Text(
                                text = if (searchQuery.isNotBlank()) highlightText(docx.title, searchQuery) else AnnotatedString(docx.title),
                                fontSize = (fontSizeSp + 6).sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                lineHeight = ((fontSizeSp + 6) * 1.3).sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Render Embedded Images (if any)
                        if (docx.images.isNotEmpty()) {
                            docx.images.take(5).forEach { imgBytes ->
                                val bmp = remember(imgBytes) {
                                    try {
                                        BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
                                    } catch (_: Exception) { null }
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Document Image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .padding(vertical = 8.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        // Render Formatted Paragraphs
                        docx.paragraphs.forEach { para ->
                            if (para.isHeading) {
                                val hSize = when (para.headingLevel) {
                                    1 -> fontSizeSp + 4
                                    2 -> fontSizeSp + 2
                                    else -> fontSizeSp + 1
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) highlightText(para.text, searchQuery) else AnnotatedString(para.text),
                                    fontSize = hSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (readingThemeIndex == 2) Color(0xFF90CAF9) else Color(0xFF1565C0),
                                    lineHeight = (hSize * 1.3).sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            } else if (para.isBullet) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        fontSize = (fontSizeSp + 1).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) highlightText(para.text, searchQuery) else AnnotatedString(para.text),
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp * 1.35).sp,
                                        color = textColor
                                    )
                                }
                            } else {
                                Text(
                                    text = if (searchQuery.isNotBlank()) highlightText(para.text, searchQuery) else AnnotatedString(para.text),
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 1.35).sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Render Tables (if any)
                        if (docx.tables.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            docx.tables.forEach { table ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = bgColor),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Column {
                                        table.rows.forEachIndexed { rowIdx, row ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (rowIdx == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                row.forEach { cell ->
                                                    Text(
                                                        text = cell,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = if (rowIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                        color = textColor,
                                                        fontSize = (fontSizeSp - 1).coerceAtLeast(10).sp,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(horizontal = 4.dp)
                                                    )
                                                }
                                            }
                                            if (rowIdx < table.rows.size - 1) {
                                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!state.previewText.isNullOrBlank()) {
                        // Fallback text rendering
                        Text(
                            text = if (searchQuery.isNotBlank()) highlightText(state.previewText, searchQuery) else AnnotatedString(state.previewText),
                            color = textColor,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.35).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Document Stats: $wordCount words • $charCount characters",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun parseSlideColor(hex: String?, defaultColor: Color): Color {
    if (hex.isNullOrBlank()) return defaultColor
    return try {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6 || clean.length == 8) {
            Color(android.graphics.Color.parseColor("#$clean"))
        } else {
            defaultColor
        }
    } catch (_: Exception) {
        defaultColor
    }
}

private fun isColorLight(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}

@Composable
private fun TextViewer(
    state: ViewerUiState.TextReady,
    onCopySensitiveText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var fontSizeSp by remember { mutableIntStateOf(14) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { if (fontSizeSp > 10) fontSizeSp -= 2 },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("A-", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = { if (fontSizeSp < 26) fontSizeSp += 2 },
                    modifier = Modifier.size(34.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("A+", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("${fontSizeSp}sp", style = MaterialTheme.typography.labelSmall)
            }

            OutlinedButton(onClick = {
                onCopySensitiveText(state.textContent)
                Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy All")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Find in text / code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) highlightText(state.textContent, searchQuery) else AnnotatedString(state.textContent),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.4).sp
            )
        }
    }
}

private fun highlightText(fullText: String, query: String, highlightColor: Color = Color(0xFFFFD54F)): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(fullText)
    return buildAnnotatedString {
        var startIndex = 0
        while (startIndex < fullText.length) {
            val index = fullText.indexOf(query, startIndex, ignoreCase = true)
            if (index == -1) {
                append(fullText.substring(startIndex))
                break
            }
            append(fullText.substring(startIndex, index))
            withStyle(style = SpanStyle(background = highlightColor, color = Color.Black)) {
                append(fullText.substring(index, index + query.length))
            }
            startIndex = index + query.length
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
