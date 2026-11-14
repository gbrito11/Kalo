package pt.guilherme.fit.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.client.HttpClient

/** Onde o estado da app é gravado (um ficheiro JSON). */
interface Storage {
    fun read(): String?
    fun write(content: String)
}

data class Prediction(val label: String, val confidence: Double)

/** Modelo de visão a correr no telemóvel (ONNX no Android, Core ML no iPhone). */
interface LocalFoodClassifier {
    val isAvailable: Boolean
    suspend fun classify(jpeg: ByteArray): List<Prediction>
}

data class HealthSnapshot(
    val steps: Long? = null,
    val activeKcal: Double? = null,
    val exerciseMinutes: Double? = null,
    val latestWeightKg: Double? = null,
)

/** Apple Health (iOS) ou Health Connect (Android). */
interface HealthBridge {
    val sourceName: String
    fun isAvailable(): Boolean
    suspend fun requestPermissions(): Boolean
    suspend fun readToday(): HealthSnapshot
    suspend fun writeWater(ml: Int)
    suspend fun writeWeight(kg: Double)
    suspend fun writeNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double)
}

object NoHealth : HealthBridge {
    override val sourceName = "Saúde"
    override fun isAvailable() = false
    override suspend fun requestPermissions() = false
    override suspend fun readToday() = HealthSnapshot()
    override suspend fun writeWater(ml: Int) {}
    override suspend fun writeWeight(kg: Double) {}
    override suspend fun writeNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double) {}
}

class PlatformServices(
    val storage: Storage,
    val health: HealthBridge,
    val classifier: LocalFoodClassifier,
    val http: HttpClient,
)

interface ImagePickerLauncher {
    fun takePhoto()
    fun pickFromGallery()
}

/** Abre a câmara ou a galeria e devolve a imagem em JPEG (ou null se cancelado). */
@Composable
expect fun rememberImagePicker(onImage: (ByteArray?) -> Unit): ImagePickerLauncher

expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** Dia de hoje (fuso local) em epochDay. */
expect fun todayEpochDay(): Long

expect fun currentTimeMillis(): Long

/** Hora local (0-23). */
expect fun localHour(): Int

@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
