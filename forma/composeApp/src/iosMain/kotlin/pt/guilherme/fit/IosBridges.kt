package pt.guilherme.fit.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import kotlin.coroutines.resume

/**
 * Implementado em Swift (iosApp/Forma/HealthKitProvider.swift) com o HealthKit.
 * Valores negativos significam "sem dados".
 */
interface IosHealthProvider {
    fun isAvailable(): Boolean
    fun requestAuthorization(completion: (Boolean) -> Unit)
    fun todaySteps(completion: (Double) -> Unit)
    fun todayActiveEnergy(completion: (Double) -> Unit)
    fun todayExerciseMinutes(completion: (Double) -> Unit)
    fun latestWeight(completion: (Double) -> Unit)
    fun saveWater(ml: Double)
    fun saveWeight(kg: Double)
    fun saveNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double)
}

class ClassifierResult(val label: String, val confidence: Double)

/** Implementado em Swift (iosApp/Forma/CoreMLFoodClassifier.swift) com Core ML + Vision. */
interface IosFoodClassifier {
    fun isModelAvailable(): Boolean
    fun classify(jpeg: NSData, completion: (List<ClassifierResult>) -> Unit)
}

class IosHealthBridge(private val provider: IosHealthProvider) : HealthBridge {
    override val sourceName = "Apple Health"
    override fun isAvailable(): Boolean = provider.isAvailable()

    override suspend fun requestPermissions(): Boolean = suspendCancellableCoroutine { cont ->
        provider.requestAuthorization { ok -> if (cont.isActive) cont.resume(ok) }
    }

    private suspend fun read(block: ((Double) -> Unit) -> Unit): Double? = suspendCancellableCoroutine { cont ->
        block { v -> if (cont.isActive) cont.resume(if (v < 0) null else v) }
    }

    override suspend fun readToday(): HealthSnapshot = HealthSnapshot(
        steps = read { provider.todaySteps(it) }?.toLong(),
        activeKcal = read { provider.todayActiveEnergy(it) },
        exerciseMinutes = read { provider.todayExerciseMinutes(it) },
        latestWeightKg = read { provider.latestWeight(it) },
    )

    override suspend fun writeWater(ml: Int) = provider.saveWater(ml.toDouble())
    override suspend fun writeWeight(kg: Double) = provider.saveWeight(kg)
    override suspend fun writeNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double) =
        provider.saveNutrition(kcal, protein, carbs, fat)
}

class IosClassifierAdapter(private val classifier: IosFoodClassifier) : LocalFoodClassifier {
    override val isAvailable: Boolean get() = classifier.isModelAvailable()

    override suspend fun classify(jpeg: ByteArray): List<Prediction> = suspendCancellableCoroutine { cont ->
        classifier.classify(jpeg.toNSData()) { results ->
            if (cont.isActive) cont.resume(results.map { Prediction(it.label, it.confidence) })
        }
    }
}
