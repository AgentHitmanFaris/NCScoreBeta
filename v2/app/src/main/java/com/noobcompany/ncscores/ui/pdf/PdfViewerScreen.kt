package com.noobcompany.ncscores.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noobcompany.ncscores.ui.theme.DarkBackground
import com.noobcompany.ncscores.ui.theme.DarkSurface
import com.noobcompany.ncscores.ui.theme.PremiumGold
import com.noobcompany.ncscores.ui.theme.TextPrimary
import com.noobcompany.ncscores.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.security.MessageDigest

@Composable
fun PdfViewerScreen(
    pdfUrl: String,
    songTitle: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var loadedFilePath by remember { mutableStateOf<String?>(null) }
    var loadingError by remember { mutableStateOf<String?>(null) }

    // Unique cached file computation
    val cachedFile = remember(pdfUrl) {
        val hash = MessageDigest.getInstance("MD5")
            .digest(pdfUrl.toByteArray())
            .joinToString("") { "%02x".format(it) }
        File(context.cacheDir, "$hash.pdf")
    }

    // Interactive rendering scaling factors
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Download pipeline
    LaunchedEffect(pdfUrl) {
        if (cachedFile.exists()) {
            loadedFilePath = cachedFile.absolutePath
            downloadProgress = 1f
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val urlRef = URL(pdfUrl)
                    val connection = urlRef.openConnection()
                    connection.connect()
                    
                    val contentLength = connection.contentLength
                    val input = connection.getInputStream()
                    val output = cachedFile.outputStream()
                    
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesLoaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesLoaded += bytesRead
                        if (contentLength > 0) {
                            withContext(Dispatchers.Main) {
                                downloadProgress = totalBytesLoaded.toFloat() / contentLength.toFloat()
                            }
                        }
                    }
                    output.flush()
                    output.close()
                    input.close()

                    withContext(Dispatchers.Main) {
                        loadedFilePath = cachedFile.absolutePath
                        downloadProgress = 1f
                    }
                } catch (e: Exception) {
                    Log.e("PdfViewer", "Cache download error", e)
                    withContext(Dispatchers.Main) {
                        loadingError = e.localizedMessage ?: "Network download failed"
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("pdf_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = PremiumGold
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = songTitle,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Native Score Renderer",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Inline zoom state tools
                IconButton(onClick = { zoomScale = (zoomScale + 0.3f).coerceAtMost(3f) }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = PremiumGold)
                }
                IconButton(onClick = { 
                    zoomScale = (zoomScale - 0.3f).coerceAtOnce(1f) 
                    if (zoomScale == 1f) panOffset = Offset.Zero
                }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = PremiumGold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            when {
                loadingError != null -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to Download Music Sheet",
                            color = Color.Red,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = loadingError!!,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                loadedFilePath == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PremiumGold,
                            progress = { downloadProgress },
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Caching digital sheet score... ${(downloadProgress * 100).toInt()}%",
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    // Start Native PDF Rendering Engine
                    PdfRawViewport(
                        filePath = loadedFilePath!!,
                        scale = zoomScale,
                        offset = panOffset,
                        onTransform = { updatedScale, updatedOffset ->
                            zoomScale = updatedScale.coerceIn(1f, 3f)
                            panOffset = if (zoomScale > 1f) updatedOffset else Offset.Zero
                        }
                    )
                }
            }
        }
    }
}

/**
 * Native rendering component supporting pinch-to-zoom and dragging.
 */
@Composable
fun PdfRawViewport(
    filePath: String,
    scale: Float,
    offset: Offset,
    onTransform: (Float, Offset) -> Unit
) {
    val context = LocalContext.current
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    val bitmaps = remember { mutableStateListOf<Bitmap?>() }

    // Load file descriptor safely
    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(descriptor)
                
                withContext(Dispatchers.Main) {
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount
                    bitmaps.clear()
                    repeat(pageCount) { bitmaps.add(null) }
                }
            } catch (e: Exception) {
                Log.e("PdfRenderer", "Initialization failed", e)
            }
        }
    }

    // Lazy load page bitmaps on scroll
    LaunchedEffect(pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            for (i in 0 until pageCount) {
                try {
                    val page = renderer.openPage(i)
                    // Scale bitmap up to match clean DPI specifications
                    val targetWidth = context.resources.displayMetrics.widthPixels
                    val targetHeight = (targetWidth * page.height / page.width)
                    
                    val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    withContext(Dispatchers.Main) {
                        bitmaps[i] = bitmap
                    }
                } catch (e: Exception) {
                    Log.e("PdfRenderer", "Failed loading page $i", e)
                }
            }
        }
    }

    // Close renderer
    DisposableEffect(filePath) {
        onDispose {
            try {
                pdfRenderer?.close()
            } catch (e: Exception) {
                Log.e("PdfRenderer", "Failed to close safely", e)
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = scale * zoom
                    val nextOffset = offset + pan
                    onTransform(nextScale, nextOffset)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Reset transforms
                        onTransform(1f, Offset.Zero)
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(pageCount) { index ->
                val bitmapSnapshot = bitmaps.getOrNull(index)
                if (bitmapSnapshot != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Image(
                            bitmap = bitmapSnapshot.asImageBitmap(),
                            contentDescription = "Sheet Music Page ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(4.dp))
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Page ${index + 1} of $pageCount",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp)
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = PremiumGold, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = "Unpacking page ${index + 1}...", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

// Float boundary helper
fun Float.coerceAtOnce(minimumValue: Float): Float = if (this < minimumValue) minimumValue else this
