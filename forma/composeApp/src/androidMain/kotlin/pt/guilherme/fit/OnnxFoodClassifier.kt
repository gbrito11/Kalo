package pt.guilherme.fit.platform

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

/**
 * Corre o modelo treinado em ml/ (assets/food_model.onnx + assets/labels.txt).
 * Entrada: [1, 3, 224, 224] com valores 0-255 (a normalização está dentro do modelo).
 * Saída: probabilidades por classe.
 */
class OnnxFoodClassifier(context: Context) : LocalFoodClassifier {
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()

    private val session: OrtSession? = runCatching {
        val bytes = context.assets.open("food_model.onnx").use { it.readBytes() }
        env.createSession(bytes, OrtSession.SessionOptions())
    }.getOrNull()

    private val labels: List<String> = runCatching {
        context.assets.open("labels.txt").bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }.getOrDefault(emptyList())

    override val isAvailable: Boolean get() = session != null && labels.isNotEmpty()

    override suspend fun classify(jpeg: ByteArray): List<Prediction> = withContext(Dispatchers.Default) {
        val s = session ?: return@withContext emptyList()
        val src = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return@withContext emptyList()
        val side = minOf(src.width, src.height)
        val square = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
        val img = Bitmap.createScaledBitmap(square, SIZE, SIZE, true)

        val pixels = IntArray(SIZE * SIZE)
        img.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE)
        val buf = FloatBuffer.allocate(3 * SIZE * SIZE)
        for (channel in 0 until 3) {
            val shift = when (channel) { 0 -> 16; 1 -> 8; else -> 0 }
            for (p in pixels) buf.put(((p shr shift) and 0xFF).toFloat())
        }
        buf.rewind()

        synchronized(this@OnnxFoodClassifier) {
            OnnxTensor.createTensor(env, buf, longArrayOf(1, 3, SIZE.toLong(), SIZE.toLong())).use { input ->
                s.run(mapOf(s.inputNames.first() to input)).use { result ->
                    @Suppress("UNCHECKED_CAST")
                    val probs = (result[0].value as Array<FloatArray>)[0]
                    probs.indices.sortedByDescending { probs[it] }.take(5)
                        .mapNotNull { i -> labels.getOrNull(i)?.let { Prediction(it, probs[i].toDouble()) } }
                }
            }
        }
    }

    private companion object { const val SIZE = 224 }
}
