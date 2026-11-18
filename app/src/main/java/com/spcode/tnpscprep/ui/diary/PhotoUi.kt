package com.spcode.tnpscprep.ui.diary

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

/** One photo in an entry. [onRemove] null = read only. */
@Composable
fun PhotoBlockView(name: String, meta: DiaryPhotoEntity?, store: PhotoStore, onRemove: (() -> Unit)?) {
    val bmp by produceState<ImageBitmap?>(null, name) { value = PhotoBitmaps.get(store, name)?.asImageBitmap() }
    val ratio = (meta?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: bmp?.let { it.width.toFloat() / it.height } ?: 1.33f).coerceIn(0.6f, 2.4f)
    val shape = if (meta?.frame == "ROUNDED") RoundedCornerShape((meta.cornerDp).dp) else RoundedCornerShape(0.dp)
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).aspectRatio(ratio).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        val b = bmp
        if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(stringResource(R.string.photo_missing), Modifier.align(Alignment.Center).padding(16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onRemove != null) IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(34.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))) {
            Icon(Icons.Rounded.Close, stringResource(R.string.photo_remove), Modifier.size(18.dp), tint = Color.White)
        }
    }
}

/** A run of text between photos. Reports its cursor so a new photo can be placed exactly there. */
@Composable
fun EditorTextBlock(text: String, style: TextStyle, hint: String?, onText: (String) -> Unit, onCursor: (Int) -> Unit, onFocus: () -> Unit, modifier: Modifier = Modifier) {
    var tfv by remember { mutableStateOf(TextFieldValue(text, TextRange(text.length))) }
    LaunchedEffect(text) { if (tfv.text != text) tfv = TextFieldValue(text, TextRange(text.length)) }
    BasicTextField(
        value = tfv,
        onValueChange = { tfv = it; onCursor(it.selection.start); if (it.text != text) onText(it.text) },
        textStyle = style, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.fillMaxWidth().onFocusChanged { if (it.isFocused) { onFocus(); onCursor(tfv.selection.start) } },
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
                        onDone(out, if (corner >= 1f) "ROUNDED" else "ORIGINAL", corner.toInt())
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
                val shape = if (mode == CropShape.ORIGINAL) RoundedCornerShape(0.dp) else RoundedCornerShape(corner.dp)
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
                CropShape.entries.forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(stringResource(m.label)) }) }
            }
            if (mode != CropShape.ORIGINAL) Column(Modifier.padding(horizontal = 20.dp)) {
                Text(stringResource(R.string.photo_round), style = MaterialTheme.typography.labelLarge)
                Slider(value = corner, onValueChange = { corner = it }, valueRange = 0f..48f)
            } else Spacer(Modifier.height(8.dp))
        }
    }
}
