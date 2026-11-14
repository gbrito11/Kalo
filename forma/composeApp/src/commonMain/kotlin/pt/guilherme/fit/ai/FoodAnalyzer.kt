package pt.guilherme.fit.ai

import pt.guilherme.fit.data.Food101
import pt.guilherme.fit.model.AnalyzerMode
import pt.guilherme.fit.model.EntrySource
import pt.guilherme.fit.model.Macros
import pt.guilherme.fit.model.Settings
import pt.guilherme.fit.platform.LocalFoodClassifier

data class Alternative(val name: String, val grams: Double?, val macros: Macros, val confidence: Double)

data class FoodGuess(
    val name: String,
    val grams: Double?,
    val macros: Macros,
    val confidence: Double?,
    val source: EntrySource,
    val alternatives: List<Alternative> = emptyList(),
)

data class AnalysisResult(
    val items: List<FoodGuess>,
    val message: String? = null,
)

/** Decide se usa o modelo local, o Gemini, ou os dois, conforme as definições. */
class FoodAnalyzer(
    private val local: LocalFoodClassifier,
    private val gemini: GeminiClient,
) {
    suspend fun analyze(jpeg: ByteArray, settings: Settings): AnalysisResult = when (settings.analyzerMode) {
        AnalyzerMode.GEMINI -> runGemini(jpeg, settings)
        AnalyzerMode.LOCAL -> runLocal(jpeg)
        AnalyzerMode.LOCAL_THEN_GEMINI -> {
            val localResult = runLocal(jpeg)
            val conf = localResult.items.firstOrNull()?.confidence ?: 0.0
            val canUseGemini = settings.geminiApiKey.isNotBlank()
            if ((localResult.items.isEmpty() || conf < settings.localConfidenceThreshold) && canUseGemini) {
                try {
                    val g = runGemini(jpeg, settings)
                    g.copy(message = listOfNotNull("Modelo local com pouca certeza, usei o Gemini.", g.message).joinToString("\n"))
                } catch (e: GeminiException) {
                    localResult.copy(message = listOfNotNull(localResult.message, e.message).joinToString("\n"))
                }
            } else localResult
        }
    }

    private suspend fun runGemini(jpeg: ByteArray, s: Settings): AnalysisResult {
        val r = gemini.analyzeFood(jpeg, s.geminiApiKey, s.geminiModel)
        val items = r.items.filter { it.name.isNotBlank() }.map {
            FoodGuess(it.name, it.grams, Macros(it.kcal, it.protein, it.carbs, it.fat), r.confidence, EntrySource.PHOTO_GEMINI)
        }
        return AnalysisResult(items, r.notes?.takeIf { it.isNotBlank() } ?: if (items.isEmpty()) "Não encontrei comida na foto." else null)
    }

    private suspend fun runLocal(jpeg: ByteArray): AnalysisResult {
        if (!local.isAvailable) {
            return AnalysisResult(emptyList(), "O modelo local ainda não está instalado. Treina-o com a pasta ml/ (vê o README) ou usa o Gemini.")
        }
        val preds = local.classify(jpeg).sortedByDescending { it.confidence }.take(4)
        if (preds.isEmpty()) return AnalysisResult(emptyList(), "O modelo local não reconheceu nada.")
        val options = preds.map { p ->
            val info = Food101.get(p.label)
            Alternative(info?.namePt ?: p.label.replace('_', ' '), info?.portionGrams, info?.portion ?: Macros(), p.confidence)
        }
        val top = options.first()
        val guess = FoodGuess(top.name, top.grams, top.macros, top.confidence, EntrySource.PHOTO_LOCAL, options.drop(1))
        val msg = if (top.confidence < 0.4) "Pouca certeza: confirma o prato ou escolhe uma alternativa." else null
        return AnalysisResult(listOf(guess), msg)
    }
}
