package pt.guilherme.fit.ai

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class GeminiException(message: String, val code: Int = 0) : Exception(message)

@Serializable
data class GeminiFoodItem(
    val name: String = "",
    val grams: Double? = null,
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
)

@Serializable
data class GeminiFoodResponse(
    val items: List<GeminiFoodItem> = emptyList(),
    val confidence: Double? = null,
    val notes: String? = null,
)

/** Cliente mínimo da API REST do Gemini (plano gratuito do Google AI Studio). */
class GeminiClient(private val http: HttpClient) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val foodPrompt = """
        Analisa a fotografia de comida. Identifica cada alimento visível, estima o peso em gramas da porção
        que aparece na imagem e calcula as calorias e os macronutrientes (proteína, hidratos de carbono e gordura, em gramas).
        Usa nomes em português de Portugal. Se for um prato conhecido (ex.: francesinha, bacalhau à Brás), usa esse nome.
        Considera o azeite/óleo provável da confeção. Se não houver comida, devolve items vazio.
        Responde APENAS com JSON neste formato:
        {"items":[{"name":"","grams":0,"kcal":0,"protein":0,"carbs":0,"fat":0}],"confidence":0.0,"notes":""}
        "confidence" é a tua certeza de 0 a 1. "notes" é uma frase curta (ex.: dificuldade em ver o molho).
    """.trimIndent()

    private suspend fun generate(apiKey: String, model: String, parts: List<JsonObject>, jsonMode: Boolean): String {
        if (apiKey.isBlank()) throw GeminiException("Falta a chave da API do Gemini (Perfil → Definições).")
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") { parts.forEach { add(it) } }
                }
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                if (jsonMode) put("responseMimeType", "application/json")
            }
        }
        val resp = http.post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent") {
            header("x-goog-api-key", apiKey.trim())
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
        val text = resp.bodyAsText()
        if (!resp.status.isSuccess()) {
            val msg = runCatching {
                json.parseToJsonElement(text).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            }.getOrNull()
            val friendly = when (resp.status.value) {
                429 -> "Limite gratuito do Gemini atingido. Tenta mais tarde ou usa o modelo local."
                400, 403 -> "Chave da API inválida ou sem acesso a este modelo."
                404 -> "Modelo \"$model\" não encontrado. Muda o nome do modelo nas definições."
                else -> "Erro do Gemini (${resp.status.value})."
            }
            throw GeminiException(if (msg != null) "$friendly\n$msg" else friendly, resp.status.value)
        }
        val root = json.parseToJsonElement(text).jsonObject
        val candidate = root["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
            ?: throw GeminiException("O Gemini não devolveu resposta (pode ter sido bloqueada).")
        return candidate["content"]?.jsonObject?.get("parts")?.jsonArray
            ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
            ?.joinToString("")
            ?.takeIf { it.isNotBlank() }
            ?: throw GeminiException("Resposta vazia do Gemini.")
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun analyzeFood(jpeg: ByteArray, apiKey: String, model: String): GeminiFoodResponse {
        val parts = listOf(
            buildJsonObject {
                putJsonObject("inlineData") {
                    put("mimeType", "image/jpeg")
                    put("data", Base64.encode(jpeg))
                }
            },
            buildJsonObject { put("text", foodPrompt) },
        )
        val raw = generate(apiKey, model, parts, jsonMode = true)
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return try {
            json.decodeFromString(GeminiFoodResponse.serializer(), cleaned)
        } catch (e: Exception) {
            throw GeminiException("Não consegui ler a resposta do Gemini.")
        }
    }

    suspend fun ask(apiKey: String, model: String, prompt: String): String =
        generate(apiKey, model, listOf(buildJsonObject { put("text", prompt) }), jsonMode = false)
}
