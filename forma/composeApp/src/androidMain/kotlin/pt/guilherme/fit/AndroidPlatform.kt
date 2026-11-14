package pt.guilherme.fit.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class AndroidStorage(context: Context) : Storage {
    private val file = File(context.filesDir, "forma_state.json")
    override fun read(): String? = if (file.exists()) file.readText() else null
    override fun write(content: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(content)
        tmp.renameTo(file)
    }
}

fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        }
    }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? =
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()

actual fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localHour(): Int = LocalTime.now().hour

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled, onBack)

/** Lê a imagem, reduz para no máximo 1600 px e corrige a rotação EXIF. */
internal fun readAndShrink(context: Context, uri: Uri): ByteArray? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
    val bmp = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    }
    val rotation = context.contentResolver.openInputStream(uri)?.use {
        when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    bmp?.let { b ->
        val fixed = if (rotation != 0f) Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(rotation) }, true) else b
        ByteArrayOutputStream().use { out ->
            fixed.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.toByteArray()
        }
    }
} catch (e: Exception) {
    null
}

@Composable
actual fun rememberImagePicker(onImage: (ByteArray?) -> Unit): ImagePickerLauncher {
    val context = LocalContext.current
    val currentOnImage by rememberUpdatedState(onImage)
    val pending = remember { arrayOfNulls<Uri>(1) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pending[0]
        currentOnImage(if (ok && uri != null) readAndShrink(context, uri) else null)
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        currentOnImage(uri?.let { readAndShrink(context, it) })
    }
    return remember(camera, gallery) {
        object : ImagePickerLauncher {
            override fun takePhoto() {
                val dir = File(context.cacheDir, "photos").apply { mkdirs() }
                val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                pending[0] = uri
                camera.launch(uri)
            }

            override fun pickFromGallery() {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}
