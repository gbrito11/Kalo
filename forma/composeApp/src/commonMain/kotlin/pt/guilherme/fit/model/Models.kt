package pt.guilherme.fit.model

import kotlinx.serialization.Serializable

// ---------- Perfil e objetivos ----------

@Serializable
enum class Sex(val label: String) { MALE("Masculino"), FEMALE("Feminino") }

@Serializable
enum class ActivityLevel(val factor: Double, val label: String, val hint: String) {
    SEDENTARY(1.2, "Sedentário", "Trabalho sentado, pouco exercício"),
    LIGHT(1.375, "Leve", "Exercício 1-3x por semana"),
    MODERATE(1.55, "Moderado", "Exercício 3-5x por semana"),
    ACTIVE(1.725, "Ativo", "Exercício 6-7x por semana"),
    VERY_ACTIVE(1.9, "Muito ativo", "Trabalho físico + treino diário"),
}

@Serializable
enum class Goal(val label: String) { LOSE("Perder gordura"), MAINTAIN("Manter"), GAIN("Ganhar massa") }

@Serializable
enum class DietType(val label: String) {
    BALANCED("Equilibrada"),
    MEDITERRANEAN("Mediterrânica"),
    HIGH_PROTEIN("Rica em proteína"),
    LOW_CARB("Low carb"),
    KETO("Cetogénica"),
    VEGETARIAN("Vegetariana"),
    VEGAN("Vegan"),
    PESCATARIAN("Pescetariana"),
}

/** Alergias / intolerâncias que excluem receitas e alimentos. */
@Serializable
enum class Restriction(val label: String) {
    GLUTEN("Glúten"),
    LACTOSE("Lactose"),
    NUTS("Frutos secos"),
    PEANUT("Amendoim"),
    SHELLFISH("Marisco"),
    FISH("Peixe"),
    EGG("Ovo"),
    SOY("Soja"),
}

@Serializable
enum class Experience(val label: String) { BEGINNER("Iniciante"), INTERMEDIATE("Intermédio"), ADVANCED("Avançado") }

@Serializable
data class Profile(
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 21,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val targetWeightKg: Double = 70.0,
    val activity: ActivityLevel = ActivityLevel.LIGHT,
    val goal: Goal = Goal.LOSE,
    /** kg por semana a perder/ganhar (0.25 a 1.0) */
    val weeklyRateKg: Double = 0.5,
    val dietType: DietType = DietType.MEDITERRANEAN,
    val restrictions: Set<Restriction> = emptySet(),
    val dislikes: List<String> = emptyList(),
    /** tempo máximo para preparar uma refeição */
    val maxPrepMinutes: Int = 30,
    val mealsPerDay: Int = 4,
    val trainingDaysPerWeek: Int = 3,
    val sessionMinutes: Int = 60,
    val experience: Experience = Experience.BEGINNER,
)

// ---------- Registo diário ----------

@Serializable
enum class MealSlot(val label: String) {
    BREAKFAST("Pequeno-almoço"), LUNCH("Almoço"), SNACK("Lanche"), DINNER("Jantar")
}

@Serializable
enum class EntrySource { MANUAL, DATABASE, RECIPE, PHOTO_GEMINI, PHOTO_LOCAL }

@Serializable
data class Macros(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
) {
    operator fun plus(o: Macros) = Macros(kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat)
    operator fun times(f: Double) = Macros(kcal * f, protein * f, carbs * f, fat * f)
}

@Serializable
data class FoodEntry(
    val id: String,
    /** dia em epochDay (dias desde 1970-01-01) */
    val day: Long,
    val slot: MealSlot,
    val name: String,
    val grams: Double? = null,
    val macros: Macros,
    val source: EntrySource = EntrySource.MANUAL,
)

@Serializable
data class WaterEntry(val id: String, val day: Long, val ml: Int)

@Serializable
data class WeightEntry(val day: Long, val kg: Double)

// ---------- Despensa e equipamento ----------

@Serializable
data class PantryItem(val id: String, val name: String, val note: String = "")

@Serializable
enum class KitchenTool(val label: String) {
    STOVE("Fogão / placa"),
    OVEN("Forno"),
    MICROWAVE("Micro-ondas"),
    AIR_FRYER("Air fryer"),
    BLENDER("Liquidificadora / varinha"),
    GRILL_PAN("Grelhador"),
    PRESSURE_COOKER("Panela de pressão"),
    TOASTER("Torradeira"),
}

@Serializable
enum class GymEquipment(val label: String) {
    BODYWEIGHT("Peso corporal"),
    DUMBBELLS("Halteres"),
    BARBELL("Barra + discos"),
    BENCH("Banco"),
    SQUAT_RACK("Rack de agachamento"),
    PULLUP_BAR("Barra de elevações"),
    BANDS("Elásticos"),
    KETTLEBELL("Kettlebell"),
    CABLES("Polias / cabos"),
    MACHINES("Máquinas de ginásio"),
    CARDIO_MACHINE("Passadeira / bicicleta / remo"),
}

// ---------- Treino ----------

@Serializable
enum class Muscle(val label: String) {
    CHEST("Peito"), BACK("Costas"), SHOULDERS("Ombros"), BICEPS("Bíceps"), TRICEPS("Tríceps"),
    QUADS("Quadríceps"), HAMSTRINGS("Posteriores"), GLUTES("Glúteos"), CALVES("Gémeos"),
    CORE("Core"), FULL("Corpo inteiro"),
}

@Serializable
enum class Pattern {
    SQUAT, HINGE, LUNGE, H_PUSH, V_PUSH, H_PULL, V_PULL, ISO_ARMS_BI, ISO_ARMS_TRI,
    ISO_SHOULDER, ISO_LEGS, CALF, CORE, CARDIO
}

@Serializable
data class PlannedExercise(
    val exerciseId: String,
    val sets: Int,
    val reps: String,
    val restSec: Int,
)

@Serializable
data class WorkoutSession(
    val name: String,
    /** 1 = segunda ... 7 = domingo */
    val weekday: Int,
    val exercises: List<PlannedExercise>,
    val cardioMinutes: Int = 0,
)

@Serializable
data class WorkoutPlan(
    val createdDay: Long,
    val splitName: String,
    val sessions: List<WorkoutSession>,
)

@Serializable
data class WorkoutLog(
    val day: Long,
    val sessionName: String,
    val doneExercises: Set<String> = emptySet(),
    val completed: Boolean = false,
    val notes: String = "",
)

// ---------- Plano de refeições ----------

@Serializable
data class PlannedMeal(
    val day: Long,
    val slot: MealSlot,
    val recipeId: String,
    val servings: Double,
)

// ---------- Definições ----------

@Serializable
enum class AnalyzerMode(val label: String) {
    LOCAL("Modelo no telemóvel (offline)"),
    GEMINI("Gemini (cloud)"),
    LOCAL_THEN_GEMINI("Local, e Gemini se não tiver certeza"),
}

@Serializable
data class Settings(
    val analyzerMode: AnalyzerMode = AnalyzerMode.LOCAL_THEN_GEMINI,
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    /** abaixo desta confiança o modelo local passa ao Gemini (no modo LOCAL_THEN_GEMINI) */
    val localConfidenceThreshold: Double = 0.55,
    /** usa as kcal ativas do Apple Health / Health Connect em vez do fator de atividade */
    val useHealthActiveEnergy: Boolean = false,
    val syncWaterToHealth: Boolean = true,
    val syncWeightToHealth: Boolean = true,
    /** envia as calorias e macros registadas para o Apple Health / Health Connect */
    val syncFoodToHealth: Boolean = false,
)

// ---------- Estado completo da app (gravado em JSON) ----------

@Serializable
data class AppState(
    val version: Int = 1,
    val profile: Profile? = null,
    val foods: List<FoodEntry> = emptyList(),
    val water: List<WaterEntry> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val pantry: List<PantryItem> = emptyList(),
    val kitchenTools: Set<KitchenTool> = setOf(KitchenTool.STOVE, KitchenTool.OVEN, KitchenTool.MICROWAVE),
    val gymEquipment: Set<GymEquipment> = setOf(GymEquipment.BODYWEIGHT),
    val workoutPlan: WorkoutPlan? = null,
    val workoutLogs: List<WorkoutLog> = emptyList(),
    val mealPlan: List<PlannedMeal> = emptyList(),
    val customRecipes: List<Recipe> = emptyList(),
    val settings: Settings = Settings(),
)
