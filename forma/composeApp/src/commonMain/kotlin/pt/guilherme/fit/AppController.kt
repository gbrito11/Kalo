package pt.guilherme.fit

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pt.guilherme.fit.ai.FoodAnalyzer
import pt.guilherme.fit.ai.GeminiClient
import pt.guilherme.fit.data.AppStore
import pt.guilherme.fit.domain.MealPlanner
import pt.guilherme.fit.domain.Nutrition
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.domain.Targets
import pt.guilherme.fit.domain.WorkoutGenerator
import pt.guilherme.fit.model.AppState
import pt.guilherme.fit.model.EntrySource
import pt.guilherme.fit.model.FoodEntry
import pt.guilherme.fit.model.Macros
import pt.guilherme.fit.model.PantryItem
import pt.guilherme.fit.model.PlannedMeal
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Recipe
import pt.guilherme.fit.model.Settings
import pt.guilherme.fit.model.WaterEntry
import pt.guilherme.fit.model.WeightEntry
import pt.guilherme.fit.model.WorkoutLog
import pt.guilherme.fit.model.WorkoutSession
import pt.guilherme.fit.platform.HealthSnapshot
import pt.guilherme.fit.platform.PlatformServices
import pt.guilherme.fit.platform.todayEpochDay
import kotlin.math.roundToInt

/** Junta o estado, a IA e a ligação à app de saúde. Os ecrãs só falam com esta classe. */
class AppController(val services: PlatformServices, private val scope: CoroutineScope) {

    val store = AppStore(services.storage, scope)
    val state: StateFlow<AppState> get() = store.state
    val gemini = GeminiClient(services.http)
    val analyzer = FoodAnalyzer(services.classifier, gemini)

    private val _health = MutableStateFlow(HealthSnapshot())
    val health: StateFlow<HealthSnapshot> = _health.asStateFlow()

    private val _healthStatus = MutableStateFlow<String?>(null)
    val healthStatus: StateFlow<String?> = _healthStatus.asStateFlow()

    val healthName: String get() = services.health.sourceName
    val healthAvailable: Boolean get() = services.health.isAvailable()

    fun today(): Long = todayEpochDay()

    val recipes: List<Recipe> get() = store.recipes

    fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }

    // ---------- Metas ----------

    fun targets(s: AppState = state.value): Targets? {
        val p = s.profile ?: return null
        return Nutrition.targets(p, health.value.activeKcal, s.settings.useHealthActiveEnergy)
    }

    fun eaten(day: Long, s: AppState = state.value): Macros =
        s.foods.filter { it.day == day }.fold(Macros()) { acc, e -> acc + e.macros }

    fun waterMl(day: Long, s: AppState = state.value): Int = s.water.filter { it.day == day }.sumOf { it.ml }

    // ---------- Perfil e definições ----------

    fun saveProfile(p: Profile) {
        val today = today()
        store.update { s ->
            val weights = if (s.weights.none { it.day == today }) s.weights + WeightEntry(today, p.weightKg) else s.weights
            s.copy(profile = p, weights = weights)
        }
    }

    fun updateSettings(f: (Settings) -> Settings) = store.update { it.copy(settings = f(it.settings)) }

    // ---------- Comida ----------

    fun addFood(entry: FoodEntry) {
        store.update { it.copy(foods = it.foods + entry) }
        if (state.value.settings.syncFoodToHealth && healthAvailable) {
            scope.launch {
                runCatching { services.health.writeNutrition(entry.macros.kcal, entry.macros.protein, entry.macros.carbs, entry.macros.fat) }
            }
        }
    }

    fun removeFood(id: String) = store.update { s -> s.copy(foods = s.foods.filterNot { it.id == id }) }

    fun logRecipe(recipe: Recipe, servings: Double, day: Long, slot: pt.guilherme.fit.model.MealSlot) {
        val per = RecipeEngine.info(recipe).perServing
        addFood(
            FoodEntry(
                id = AppStore.newId(), day = day, slot = slot,
                name = recipe.name + if (servings != 1.0) " (x${fmt(servings)})" else "",
                macros = per * servings, source = EntrySource.RECIPE,
            )
        )
    }

    fun logPlannedMeal(pm: PlannedMeal) {
        val r = recipe(pm.recipeId) ?: return
        logRecipe(r, pm.servings, pm.day, pm.slot)
    }

    // ---------- Água e peso ----------

    fun addWater(ml: Int, day: Long = today()) {
        store.update { it.copy(water = it.water + WaterEntry(AppStore.newId(), day, ml)) }
        if (state.value.settings.syncWaterToHealth && healthAvailable && ml > 0) {
            scope.launch { runCatching { services.health.writeWater(ml) } }
        }
    }

    fun undoWater(day: Long = today()) = store.update { s ->
        val last = s.water.lastOrNull { it.day == day } ?: return@update s
        s.copy(water = s.water - last)
    }

    fun logWeight(kg: Double, day: Long = today()) {
        store.update { s ->
            val weights = s.weights.filterNot { it.day == day } + WeightEntry(day, kg)
            s.copy(weights = weights.sortedBy { it.day }, profile = s.profile?.copy(weightKg = kg))
        }
        if (state.value.settings.syncWeightToHealth && healthAvailable) {
            scope.launch { runCatching { services.health.writeWeight(kg) } }
        }
    }

    // ---------- Saúde ----------

    fun connectHealth() {
        if (!healthAvailable) {
            _healthStatus.value = "$healthName não está disponível neste telemóvel."
            return
        }
        scope.launch {
            val ok = runCatching { services.health.requestPermissions() }.getOrDefault(false)
            _healthStatus.value = if (ok) "Ligado ao $healthName." else "Sem permissão para o $healthName."
            if (ok) refreshHealth()
        }
    }

    fun refreshHealth() {
        if (!healthAvailable) return
        scope.launch {
            runCatching { services.health.readToday() }
                .onSuccess { snap ->
                    _health.value = snap
                    // se o peso da app de saúde for diferente do último registado hoje, guarda-o
                    val kg = snap.latestWeightKg
                    val s = state.value
                    if (kg != null && kg > 0 && s.weights.none { it.day == today() } && s.profile != null &&
                        kotlin.math.abs(kg - s.profile.weightKg) >= 0.1
                    ) {
                        store.update { st ->
                            st.copy(weights = (st.weights + WeightEntry(today(), kg)).sortedBy { it.day }, profile = st.profile?.copy(weightKg = kg))
                        }
                    }
                }
                .onFailure { _healthStatus.value = "Não consegui ler o $healthName: ${it.message}" }
        }
    }

    // ---------- Treino ----------

    fun generateWorkout() = store.update { s ->
        val p = s.profile ?: return@update s
        s.copy(workoutPlan = WorkoutGenerator.generate(p, s.gymEquipment, today()))
    }

    fun sessionForDay(day: Long, s: AppState = state.value): WorkoutSession? {
        val wd = pt.guilherme.fit.domain.Dates.weekday(day)
        return s.workoutPlan?.sessions?.firstOrNull { it.weekday == wd }
    }

    fun workoutLog(day: Long, s: AppState = state.value): WorkoutLog? = s.workoutLogs.firstOrNull { it.day == day }

    fun toggleExercise(day: Long, sessionName: String, exerciseId: String) = store.update { s ->
        val log = s.workoutLogs.firstOrNull { it.day == day } ?: WorkoutLog(day, sessionName)
        val done = if (exerciseId in log.doneExercises) log.doneExercises - exerciseId else log.doneExercises + exerciseId
        s.copy(workoutLogs = s.workoutLogs.filterNot { it.day == day } + log.copy(doneExercises = done))
    }

    fun setWorkoutCompleted(day: Long, sessionName: String, completed: Boolean, notes: String) = store.update { s ->
        val log = s.workoutLogs.firstOrNull { it.day == day } ?: WorkoutLog(day, sessionName)
        s.copy(workoutLogs = s.workoutLogs.filterNot { it.day == day } + log.copy(completed = completed, notes = notes))
    }

    // ---------- Cozinha ----------

    fun addPantry(names: String) {
        val items = names.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) return
        store.update { s ->
            val existing = s.pantry.map { it.name.lowercase() }.toSet()
            s.copy(pantry = s.pantry + items.filter { it.lowercase() !in existing }.distinctBy { it.lowercase() }.map { PantryItem(AppStore.newId(), it) })
        }
    }

    fun removePantry(id: String) = store.update { s -> s.copy(pantry = s.pantry.filterNot { it.id == id }) }

    fun generateMealPlan(days: Int = 7) = store.update { s ->
        val p = s.profile ?: return@update s
        val t = targets(s) ?: return@update s
        val start = today()
        val plan = MealPlanner.plan(start, days, p, t.kcal, recipes, s.pantry, s.kitchenTools, seed = (start + s.mealPlan.size).toInt())
        s.copy(mealPlan = s.mealPlan.filter { it.day < start } + plan)
    }

    fun clearMealPlan() = store.update { it.copy(mealPlan = it.mealPlan.filter { pm -> pm.day < today() }) }

    // ---------- Coach IA ----------

    suspend fun askCoach(): String {
        val s = state.value
        val p = s.profile ?: return "Preenche o perfil primeiro."
        val t = targets(s) ?: return "Preenche o perfil primeiro."
        val d = today()
        val e = eaten(d, s)
        val h = health.value
        val session = sessionForDay(d, s)
        val lastWeights = s.weights.takeLast(14).joinToString { "${pt.guilherme.fit.domain.Dates.short(it.day)}: ${it.kg} kg" }
        val prompt = """
            És um treinador e nutricionista prático. Responde em português de Portugal, de forma direta, no máximo 6 frases curtas,
            sem linguagem de marketing. Dá 2-3 ações concretas para hoje.
            Objetivo: ${p.goal.label}, ${p.weightKg} kg → ${p.targetWeightKg} kg, ${p.weeklyRateKg} kg/semana. Dieta: ${p.dietType.label}.
            Restrições: ${p.restrictions.joinToString { it.label }.ifBlank { "nenhuma" }}. Não gosta de: ${p.dislikes.joinToString().ifBlank { "nada" }}.
            Meta hoje: ${t.kcal.roundToInt()} kcal, ${t.protein.roundToInt()} g proteína.
            Comido hoje: ${e.kcal.roundToInt()} kcal, ${e.protein.roundToInt()} g proteína. Água: ${waterMl(d, s)} / ${t.waterMl} ml.
            Passos: ${h.steps ?: "sem dados"}. Kcal ativas: ${h.activeKcal?.roundToInt() ?: "sem dados"}. Minutos de exercício: ${h.exerciseMinutes?.roundToInt() ?: "sem dados"}.
            Treino planeado hoje: ${session?.name ?: "descanso"}. Concluído: ${workoutLog(d, s)?.completed ?: false}.
            Pesos recentes: ${lastWeights.ifBlank { "sem registos" }}.
        """.trimIndent()
        return gemini.ask(s.settings.geminiApiKey, s.settings.geminiModel, prompt)
    }
}

fun fmt(x: Double, decimals: Int = 1): String {
    if (decimals <= 0) return x.roundToInt().toString()
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val v = (x * factor).roundToInt() / factor
    return if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString().replace('.', ',')
}
