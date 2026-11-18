package com.spcode.tnpscprep.ui.diary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.data.PhotoStore
import com.spcode.tnpscprep.data.db.DiaryPhotoEntity
import com.spcode.tnpscprep.domain.CropMath
import com.spcode.tnpscprep.domain.DiaryLinks
import com.spcode.tnpscprep.domain.PhotoFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Decoded diary photos, kept in a small byte-limited cache (12 MB) so scrolling never decodes the same file twice. Decoding runs off the main thread. */
object PhotoBitmaps {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) { override fun sizeOf(key: String, value: Bitmap) = value.byteCount }
    suspend fun get(store: PhotoStore, name: String, thumb: Boolean = false): Bitmap? = withContext(Dispatchers.IO) {
        val key = if (thumb) "$name:t" else name
        cache.get(key) ?: store.file(name, thumb).takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }?.also { cache.put(key, it) }
    }
    fun forget(name: String) { cache.remove(name); cache.remove("$name:t") }
}

/** One photo in an entry, in its frame. [onClick] null = read only. */
@Composable
fun PhotoBlockView(name: String, meta: DiaryPhotoEntity?, store: PhotoStore, onClick: (() -> Unit)? = null) {
    val bmp by produceState<ImageBitmap?>(null, name) { value = PhotoBitmaps.get(store, name)?.asImageBitmap() }
    val frame = PhotoFrame.of(meta?.frame)
    val ratio = if (frame.squareOnly) 1f else (meta?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: bmp?.let { it.width.toFloat() / it.height } ?: 1.33f).coerceIn(0.6f, 2.4f)
    val shape = frameShape(meta?.frame, meta?.cornerDp ?: 0)
    // circle / heart / star are always square and centred (a 0.7 width keeps a heart from being huge)
    val widthFraction = if (frame.squareOnly) 0.78f else 1f
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Box(Modifier.fillMaxWidth(widthFraction).padding(vertical = 8.dp).aspectRatio(ratio).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant).let { m -> if (onClick != null) m.clickable(onClick = onClick) else m }) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(stringResource(R.string.photo_missing), Modifier.align(Alignment.Center).padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    }
}

/**
 * A run of text between photos. Reports its cursor so a new photo/link can be placed exactly there. Links are stored as `[label](url)`
 * but SHOWN as just the (underlined) label; a tap on one opens it, a long press edits it ([onLink]).
 */
@Composable
fun EditorTextBlock(
    text: String, style: TextStyle, hint: String?, onText: (String) -> Unit, onCursor: (Int) -> Unit, onFocus: () -> Unit,
    onLink: (DiaryLinks.Link, Boolean) -> Unit, modifier: Modifier = Modifier,
) {
    var tfv by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    LaunchedEffect(text) { if (tfv.text != text) tfv = TextFieldValue(text, TextRange(text.length)) }
    val linkColor = MaterialTheme.colorScheme.primary
    val vt = remember(linkColor) {
        VisualTransformation { raw ->
            val d = DiaryLinks.display(raw.text)
            TransformedText(
                androidx.compose.ui.text.buildAnnotatedString {
                    append(d.text)
                    d.links.forEach { addStyle(androidx.compose.ui.text.SpanStyle(color = linkColor, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline), it.tStart, it.tEnd) }
                },
                object : OffsetMapping {
                    override fun originalToTransformed(offset: Int) = d.shownOffset(offset)
                    override fun transformedToOriginal(offset: Int) = d.storedOffset(offset)
                },
            )
        }
    }
    val disp = remember(text) { DiaryLinks.display(text) }
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    BasicTextField(
        value = tfv,
        onValueChange = { tfv = it; onCursor(it.selection.start); if (it.text != text) onText(it.text) },
        textStyle = style, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), visualTransformation = vt,
        onTextLayout = { layout = it },
        modifier = modifier.fillMaxWidth()
            .pointerInput(disp) {
                if (disp.links.isEmpty()) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val l = layout ?: return@awaitEachGesture
                    val hit = disp.links.firstOrNull { sl -> (sl.tStart until sl.tEnd).any { i -> runCatching { l.getBoundingBox(i).contains(down.position) }.getOrDefault(false) } } ?: return@awaitEachGesture
                    val t0 = System.nanoTime()
                    val longMs = viewConfiguration.longPressTimeoutMillis
                    val up = withTimeoutOrNull(longMs) { waitForUpOrCancellation(PointerEventPass.Initial) }
                    val elapsedMs = (System.nanoTime() - t0) / 1_000_000
                    if (up != null) { up.consume(); down.consume(); onLink(hit.link, false) }
                    else if (elapsedMs >= longMs - 20) { down.consume(); onLink(hit.link, true); waitForUpOrCancellation(PointerEventPass.Initial)?.consume() }
                }
            }
            .onFocusChanged { if (it.isFocused) { onFocus(); onCursor(tfv.selection.start) } },
        decorationBox = { inner -> Box { if (text.isEmpty() && hint != null) Text(hint, style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))); inner() } },
    )
}

private enum class CropShape(val label: Int, val ratio: Float?) { ORIGINAL(R.string.photo_original, null), SQUARE(R.string.photo_sq, 1f), R43(R.string.photo_43, 4f / 3f), R34(R.string.photo_34, 3f / 4f), R169(R.string.photo_169, 16f / 9f) }

/**
 * Full-screen crop: the frame stays still, the picture is dragged and pinched under it. "Original" keeps the whole picture as it is (no crop, no rounding).
 * Result = the cropped bitmap + the frame ("ORIGINAL" or "ROUNDED") + the corner radius in dp.
 */
@Composable
fun PhotoCropDialog(bitmap: Bitmap, onCancel: () -> Unit, onDone: (Bitmap, String, Int) -> Unit) {
    var mode by remember { mutableStateOf(CropShape.SQUARE) }
    var corner by remember { mutableFloatStateOf(16f) }
    var frameName by remember { mutableStateOf(PhotoFrame.ROUNDED) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var frame by remember { mutableStateOf(Offset(1f, 1f)) } // frame size in px, set by the layout below

    fun reset(f: Offset) { scale = CropMath.minScale(bitmap.width, bitmap.height, f.x, f.y); offX = 0f; offY = 0f }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(top = 20.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                Text(stringResource(R.string.photo_crop_title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(onClick = {
                    if (mode == CropShape.ORIGINAL) onDone(bitmap, "ORIGINAL", 0)
                    else {
                        val r = CropMath.sourceRect(bitmap.width, bitmap.height, frame.x, frame.y, scale, offX, offY)
                        val out = Bitmap.createBitmap(bitmap, r.x, r.y, r.w.coerceAtMost(bitmap.width - r.x), r.h.coerceAtMost(bitmap.height - r.y))
                        val f = if (frameName == PhotoFrame.ROUNDED && corner < 1f) PhotoFrame.ORIGINAL else frameName
                        onDone(out, f.name, if (f == PhotoFrame.ROUNDED) corner.toInt() else 0)
                    }
                }) { Text(stringResource(R.string.photo_done)) }
            }
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                val d = LocalDensity.current
                val availW = with(d) { maxWidth.toPx() }; val availH = with(d) { maxHeight.toPx() }
                val ratio = mode.ratio ?: (bitmap.width.toFloat() / bitmap.height)
                val fw: Float; val fh: Float
                if (availW / ratio <= availH) { fw = availW; fh = availW / ratio } else { fh = availH; fw = availH * ratio }
                LaunchedEffect(mode, fw, fh) { frame = Offset(fw, fh); reset(frame) }
                val shape = if (mode == CropShape.ORIGINAL) RoundedCornerShape(0.dp) else frameShape(frameName.name, corner.toInt())
                Box(
                    Modifier.size(with(d) { fw.toDp() }, with(d) { fh.toDp() }).clip(shape).background(Color.Black)
                        .pointerInput(mode, fw, fh) {
                            if (mode == CropShape.ORIGINAL) return@pointerInput
                            detectTransformGestures { _, pan, zoom, _ ->
                                val min = CropMath.minScale(bitmap.width, bitmap.height, fw, fh)
                                scale = (scale * zoom).coerceIn(min, min * 6f)
                                offX = CropMath.clampOffset(offX + pan.x, bitmap.width, scale, fw)
                                offY = CropMath.clampOffset(offY + pan.y, bitmap.height, scale, fh)
                            }
                        },
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val s = if (mode == CropShape.ORIGINAL) minOf(size.width / bitmap.width, size.height / bitmap.height) else scale
                        val dw = bitmap.width * s; val dh = bitmap.height * s
                        val ox = if (mode == CropShape.ORIGINAL) 0f else offX; val oy = if (mode == CropShape.ORIGINAL) 0f else offY
                        drawImage(image, IntOffset.Zero, IntSize(bitmap.width, bitmap.height),
                            IntOffset((size.width / 2 - dw / 2 + ox).toInt(), (size.height / 2 - dh / 2 + oy).toInt()), IntSize(dw.toInt(), dh.toInt()))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CropShape.entries.forEach { m -> FilterChip(selected = mode == m, enabled = !(frameName.squareOnly && m != CropShape.SQUARE), onClick = { mode = m }, label = { Text(stringResource(m.label)) }) }
            }
            if (mode != CropShape.ORIGINAL) {
                FrameChips(frameName) { f -> frameName = f; if (f.squareOnly) mode = CropShape.SQUARE }
                if (frameName == PhotoFrame.ROUNDED) Column(Modifier.padding(horizontal = 20.dp)) {
                    Text(stringResource(R.string.photo_round), style = MaterialTheme.typography.labelLarge)
                    Slider(value = corner, onValueChange = { corner = it }, valueRange = 0f..48f)
                }
            } else Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun frameLabel(f: PhotoFrame) = stringResource(when (f) {
    PhotoFrame.ORIGINAL -> R.string.frame_sharp; PhotoFrame.ROUNDED -> R.string.frame_rounded; PhotoFrame.CIRCLE -> R.string.frame_circle
    PhotoFrame.HEART -> R.string.frame_heart; PhotoFrame.STAR -> R.string.frame_star
})

/** The five frames as chips. */
@Composable
fun FrameChips(selected: PhotoFrame, onPick: (PhotoFrame) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoFrame.entries.forEach { f -> FilterChip(selected = selected == f, onClick = { onPick(f) }, label = { Text(frameLabel(f)) }) }
    }
}

/** Opened by tapping a photo in the editor: change its frame, move it up or down, or remove it. The picture itself is not re-cropped. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PhotoEditSheet(meta: DiaryPhotoEntity, canUp: Boolean, canDown: Boolean, onChange: (PhotoFrame, Int) -> Unit, onUp: () -> Unit, onDown: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        var corner by remember(meta.name) { mutableFloatStateOf(meta.cornerDp.toFloat().coerceAtLeast(16f)) }
        val frame = PhotoFrame.of(meta.frame)
        Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.photo_frame), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.titleMedium)
            FrameChips(frame) { f -> onChange(f, if (f == PhotoFrame.ROUNDED) corner.toInt() else 0) }
            if (frame == PhotoFrame.ROUNDED) Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.photo_round), style = MaterialTheme.typography.labelLarge)
                Slider(value = corner, onValueChange = { corner = it; onChange(PhotoFrame.ROUNDED, it.toInt()) }, valueRange = 0f..48f)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onUp, enabled = canUp) { Text(stringResource(R.string.photo_move_up)) }
                TextButton(onClick = onDown, enabled = canDown) { Text(stringResource(R.string.photo_move_down)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onRemove) { Text(stringResource(R.string.photo_remove), color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
