package pt.guilherme.fit.domain

import pt.guilherme.fit.data.FoodDb
import pt.guilherme.fit.data.TextNorm
import pt.guilherme.fit.model.DietType
import pt.guilherme.fit.model.Food
import pt.guilherme.fit.model.FoodCategory
import pt.guilherme.fit.model.KitchenTool
import pt.guilherme.fit.model.Macros
import pt.guilherme.fit.model.MealSlot
import pt.guilherme.fit.model.PantryItem
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Recipe
import pt.guilherme.fit.model.Restriction

data class RecipeInfo(
    val recipe: Recipe,
    val perServing: Macros,
    val vegan: Boolean,
    val vegetarian: Boolean,
    val pescatarian: Boolean,
    val allergens: Set<Restriction>,
) {
    val proteinShare: Double get() = if (perServing.kcal > 0) perServing.protein * 4 / perServing.kcal else 0.0
    val fatShare: Double get() = if (perServing.kcal > 0) perServing.fat * 9 / perServing.kcal else 0.0
}

data class RecipeSuggestion(
    val info: RecipeInfo,
    /** 0..1 — fração dos ingredientes (sem básicos) que já tens */
    val coverage: Double,
    val have: List<Food>,
    val missing: List<Food>,
) {
    val recipe: Recipe get() = info.recipe
}

enum class RejectReason(val label: String) {
    DIET("não encaixa na dieta"),
    RESTRICTION("tem um alergénio teu"),
    DISLIKE("tem algo de que não gostas"),
    TIME("demora demasiado"),
    TOOLS("falta equipamento de cozinha"),
}

object RecipeEngine {

    private val meatCats = setOf(FoodCategory.MEAT, FoodCategory.POULTRY, FoodCategory.PROCESSED_MEAT)
    private val fishCats = setOf(FoodCategory.FISH, FoodCategory.SEAFOOD)

    fun info(r: Recipe): RecipeInfo {
        var total = Macros()
        var vegan = true
        var vegetarian = true
        var pescatarian = true
        val allergens = mutableSetOf<Restriction>()
        for (ing in r.ingredients) {
            val food = FoodDb.get(ing.foodKey) ?: continue
            if (ing.optional) continue
            total += food.per100 * (ing.grams / 100.0)
            if (!food.vegan) vegan = false
            if (food.category in meatCats) { vegetarian = false; pescatarian = false }
            if (food.category in fishCats) vegetarian = false
            allergens += food.allergens
        }
        val per = total * (1.0 / r.servings.coerceAtLeast(1))
        return RecipeInfo(r, per, vegan, vegetarian, pescatarian, allergens)
    }

    fun fitsDiet(info: RecipeInfo, diet: DietType): Boolean = when (diet) {
        DietType.BALANCED -> true
        DietType.MEDITERRANEAN -> info.recipe.mediterranean
        DietType.HIGH_PROTEIN -> info.proteinShare >= 0.25
        DietType.LOW_CARB -> info.perServing.carbs <= 25.0
        DietType.KETO -> info.perServing.carbs <= 12.0 && info.fatShare >= 0.5
        DietType.VEGETARIAN -> info.vegetarian
        DietType.VEGAN -> info.vegan
        DietType.PESCATARIAN -> info.pescatarian
    }

    fun hasTools(r: Recipe, tools: Set<KitchenTool>): Boolean {
        if (!tools.containsAll(r.allTools)) return false
        return r.anyTools.isEmpty() || r.anyTools.any { it in tools }
    }

    /** Verifica se a receita tem algum ingrediente/nome de que a pessoa não gosta. */
    fun dislikeHit(r: Recipe, dislikes: List<String>): String? {
        val norms = dislikes.map { TextNorm.singular(TextNorm.norm(it)) }.filter { it.length >= 2 }
        if (norms.isEmpty()) return null
        val recipeName = TextNorm.norm(r.name)
        for (d in norms) {
            if (recipeName.contains(d)) return d
            for (ing in r.ingredients) {
                if (ing.optional) continue
                val food = FoodDb.get(ing.foodKey) ?: continue
                val words = (listOf(food.name, food.key.replace('_', ' ')) + food.synonyms).map { TextNorm.singular(TextNorm.norm(it)) }
                if (words.any { it == d || it.contains(d) || (d.length >= 4 && d.contains(it) && it.length >= 3) }) return d
            }
        }
        return null
    }

    fun rejectReasons(info: RecipeInfo, profile: Profile, tools: Set<KitchenTool>, respectTime: Boolean = true): List<RejectReason> {
        val out = mutableListOf<RejectReason>()
        if (!fitsDiet(info, profile.dietType)) out += RejectReason.DIET
        if (info.allergens.any { it in profile.restrictions }) out += RejectReason.RESTRICTION
        if (dislikeHit(info.recipe, profile.dislikes) != null) out += RejectReason.DISLIKE
        if (respectTime && info.recipe.minutes > profile.maxPrepMinutes) out += RejectReason.TIME
        if (!hasTools(info.recipe, tools)) out += RejectReason.TOOLS
        return out
    }

    /** O item da despensa corresponde a este alimento? */
    fun pantryHas(food: Food, pantryNorm: List<String>): Boolean {
        if (food.staple) return true
        val names = (listOf(food.name, food.key.replace('_', ' ')) + food.synonyms)
            .map { TextNorm.singular(TextNorm.norm(it)) }
        return pantryNorm.any { p ->
            names.any { n -> n == p || n.startsWith("$p ") || p.startsWith("$n ") || (p.length >= 4 && n.contains(p)) || (n.length >= 4 && p.contains(n)) }
        }
    }

    fun normPantry(pantry: List<PantryItem>): List<String> =
        pantry.map { TextNorm.singular(TextNorm.norm(it.name)) }.filter { it.isNotBlank() }

    fun coverage(r: Recipe, pantryNorm: List<String>): Triple<Double, List<Food>, List<Food>> {
        val have = mutableListOf<Food>()
        val missing = mutableListOf<Food>()
        for (ing in r.ingredients) {
            if (ing.optional) continue
            val food = FoodDb.get(ing.foodKey) ?: continue
            if (food.staple) continue
            if (pantryHas(food, pantryNorm)) have += food else missing += food
        }
        val total = have.size + missing.size
        val cov = if (total == 0) 1.0 else have.size.toDouble() / total
        return Triple(cov, have, missing)
    }

    /**
     * Receitas que podes fazer, ordenadas pelo que já tens na despensa.
     * @param slot filtra por refeição (null = todas)
     */
    fun suggest(
        recipes: List<Recipe>,
        profile: Profile,
        pantry: List<PantryItem>,
        tools: Set<KitchenTool>,
        slot: MealSlot? = null,
        respectTime: Boolean = true,
    ): List<RecipeSuggestion> {
        val pantryNorm = normPantry(pantry)
        return recipes
            .asSequence()
            .filter { slot == null || slot in it.slots }
            .map { info(it) }
            .filter { rejectReasons(it, profile, tools, respectTime).isEmpty() }
            .map { info ->
                val (cov, have, missing) = coverage(info.recipe, pantryNorm)
                RecipeSuggestion(info, cov, have, missing)
            }
            .sortedWith(compareByDescending<RecipeSuggestion> { it.coverage }.thenBy { it.missing.size }.thenBy { it.recipe.minutes })
            .toList()
    }
}
