package pt.guilherme.fit.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import pt.guilherme.fit.model.AppState
import pt.guilherme.fit.model.Recipe
import pt.guilherme.fit.platform.Storage
import pt.guilherme.fit.platform.currentTimeMillis
import kotlin.random.Random

/** Guarda todo o estado num ficheiro JSON local. Escreve com um pequeno atraso para juntar alterações. */
class AppStore(private val storage: Storage, private val scope: CoroutineScope) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var saveJob: Job? = null

    private fun load(): AppState = try {
        storage.read()?.takeIf { it.isNotBlank() }?.let { json.decodeFromString(AppState.serializer(), it) } ?: AppState()
    } catch (e: Exception) {
        AppState()
    }

    fun update(transform: (AppState) -> AppState) {
        _state.update(transform)
        saveJob?.cancel()
        saveJob = scope.launch(Dispatchers.Default) {
            delay(400)
            saveNow()
        }
    }

    fun saveNow() {
        storage.write(json.encodeToString(AppState.serializer(), _state.value))
    }

    fun exportJson(): String = json.encodeToString(AppState.serializer(), _state.value)

    fun importJson(text: String): Boolean = try {
        val s = json.decodeFromString(AppState.serializer(), text)
        update { s }
        true
    } catch (e: Exception) {
        false
    }

    val recipes: List<Recipe> get() = builtInRecipes + _state.value.customRecipes

    companion object {
        fun newId(): String = currentTimeMillis().toString(36) + Random.nextInt(0, 1 shl 20).toString(36)
    }
}
