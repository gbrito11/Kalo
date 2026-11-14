@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package pt.guilherme.fit.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithBytes
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.min

// ---------- Conversões ----------

fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) bytes.usePinned { memcpy(it.addressOf(0), this.bytes, length) }
    return bytes
}

fun ByteArray.toNSData(): NSData = if (isEmpty()) NSData() else usePinned {
    NSData.dataWithBytes(it.addressOf(0), size.toULong())
}

// ---------- Armazenamento ----------

class IosStorage : Storage {
    private val path: String by lazy {
        val dir = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true).first() as String
        "$dir/forma_state.json"
    }

    override fun read(): String? = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)

    override fun write(content: String) {
        NSString.create(string = content).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
    }
}

fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureRequest { setTimeoutInterval(60.0) }
    }
}

// ---------- Datas ----------

actual fun todayEpochDay(): Long {
    val comps = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay, fromDate = NSDate(),
    )
    return pt.guilherme.fit.domain.Dates.toEpochDay(comps.year.toInt(), comps.month.toInt(), comps.day.toInt())
}

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun localHour(): Int =
    NSCalendar.currentCalendar.components(NSCalendarUnitHour, fromDate = NSDate()).hour.toInt()

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No iPhone usa-se o botão de voltar da barra de topo.
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? = runCatching {
    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()

// ---------- Câmara / galeria ----------

/** Redimensiona (máx. 1600 px) e "assa" a orientação na imagem, para o JPEG sair direito. */
private fun normalized(image: UIImage): UIImage {
    val (w, h) = image.size.useContents { width to height }
    val scale = min(1.0, 1600.0 / max(w, h))
    val nw = w * scale
    val nh = h * scale
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(nw, nh), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, nw, nh))
    val out = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return out ?: image
}

private class PickerDelegate(var onResult: (ByteArray?) -> Unit) :
    NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        val bytes = image?.let { UIImageJPEGRepresentation(normalized(it), 0.85)?.toByteArray() }
        onResult(bytes)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onResult(null)
    }
}

private fun topViewController(): UIViewController? {
    var vc = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) vc = vc.presentedViewController
    return vc
}

private fun present(source: UIImagePickerControllerSourceType, delegate: PickerDelegate) {
    if (!UIImagePickerController.isSourceTypeAvailable(source)) {
        delegate.onResult(null)
        return
    }
    val picker = UIImagePickerController()
    picker.sourceType = source
    picker.delegate = delegate
    topViewController()?.presentViewController(picker, animated = true, completion = null)
}

@Composable
actual fun rememberImagePicker(onImage: (ByteArray?) -> Unit): ImagePickerLauncher {
    val current by rememberUpdatedState(onImage)
    // o delegate tem de ficar referenciado enquanto o ecrã existe
    val delegate = remember { PickerDelegate { } }
    delegate.onResult = { bytes -> current(bytes) }
    return remember {
        object : ImagePickerLauncher {
            override fun takePhoto() =
                present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera, delegate)

            override fun pickFromGallery() =
                present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary, delegate)
        }
    }
}
