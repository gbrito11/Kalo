package pt.guilherme.fit.domain

import pt.guilherme.fit.data.FoodDb
import pt.guilherme.fit.model.Food
import pt.guilherme.fit.model.KitchenTool
import pt.guilherme.fit.model.MealSlot
import pt.guilherme.fit.model.PantryItem
import pt.guilherme.fit.model.PlannedMeal
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Recipe
import kotlin.math.roundToInt
import kotlin.random.Random

data class ShoppingItem(val food: Food, val grams: Double)

object MealPlanner {

    fun slotsFor(mealsPerDay: Int): List<MealSlot> = when {
        mealsPerDay <= 2 -> listOf(MealSlot.LUNCH, MealSlot.DINNER)
        mealsPerDay == 3 -> listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.DINNER)
        else -> listOf(MealSlot.BREAKFAST, MealSlot.LUNCH, MealSlot.SNACK, MealSlot.DINNER)
    }

    /** Fração das calorias diárias para cada refeição. */
    fun shares(slots: List<MealSlot>): Map<MealSlot, Double> {
        val base = mapOf(
            MealSlot.BREAKFAST to 0.25, MealSlot.LUNCH to 0.35,
            MealSlot.SNACK to 0.10, MealSlot.DINNER to 0.30,
        )
        val sum = slots.sumOf { base.getValue(it) }
        return slots.associateWith { base.getValue(it) / sum }
    }

    private fun roundQuarter(x: Double) = (x * 4).roundToInt() / 4.0

    /**
     * Gera um plano de [days] dias a partir de [startDay].
     * Dá prioridade às receitas com mais ingredientes na despensa e evita repetir receitas seguidas.
     */
    fun plan(
        startDay: Long,
        days: Int,
        profile: Profile,
        dailyKcal: Double,
        recipes: List<Recipe>,
        pantry: List<PantryItem>,
        tools: Set<KitchenTool>,
        seed: Int = startDay.toInt(),
    ): List<PlannedMeal> {
        val rnd = Random(seed)
        val slots = slotsFor(profile.mealsPerDay)
        val share = shares(slots)
        val bySlot = slots.associateWith { slot ->
            RecipeEngine.suggest(recipes, profile, pantry, tools, slot).ifEmpty {
                // sem nada dentro do tempo máximo: aceita receitas mais demoradas
                RecipeEngine.suggest(recipes, profile, pantry, tools, slot, respectTime = false)
            }
        }
        val out = mutableListOf<PlannedMeal>()
        val lastUsed = mutableMapOf<String, Long>()

        for (d in 0 until days) {
            val day = startDay + d
            val usedToday = mutableSetOf<String>()
            for (slot in slots) {
                val cands = bySlot.getValue(slot)
                if (cands.isEmpty()) continue
                val target = dailyKcal * share.getValue(slot)
                val best = cands.maxByOrNull { s ->
                    val kcal = s.info.perServing.kcal.coerceAtLeast(1.0)
                    val scale = target / kcal
                    var score = s.coverage * 2.0 + rnd.nextDouble() * 0.8
                    if (scale in 0.75..1.5) score += 0.6
                    if (scale !in 0.5..2.0) score -= 1.0
                    val last = lastUsed[s.recipe.id]
                    if (last != null) {
                        val gap = day - last
                        score -= when {
                            gap <= 0L -> 5.0
                            gap == 1L -> 2.0
                            gap == 2L -> 1.0
                            else -> 0.0
                        }
                    }
                    if (s.recipe.id in usedToday) score -= 5.0
                    score
                } ?: continue
                val servings = roundQuarter((target / best.info.perServing.kcal.coerceAtLeast(1.0)).coerceIn(0.5, 2.0))
                out += PlannedMeal(day, slot, best.recipe.id, servings)
                lastUsed[best.recipe.id] = day
                usedToday += best.recipe.id
            }
        }
        return out
    }

    /** Lista de compras: o que falta na despensa para cumprir o plano. */
    fun shoppingList(plan: List<PlannedMeal>, recipes: List<Recipe>, pantry: List<PantryItem>): List<ShoppingItem> {
        val byId = recipes.associateBy { it.id }
        val pantryNorm = RecipeEngine.normPantry(pantry)
        val grams = mutableMapOf<String, Double>()
        for (pm in plan) {
            val r = byId[pm.recipeId] ?: continue
            val factor = pm.servings / r.servings.coerceAtLeast(1)
            for (ing in r.ingredients) {
                if (ing.optional) continue
                val food = FoodDb.get(ing.foodKey) ?: continue
                if (food.staple || RecipeEngine.pantryHas(food, pantryNorm)) continue
                grams[food.key] = (grams[food.key] ?: 0.0) + ing.grams * factor
            }
        }
        return grams.mapNotNull { (k, g) -> FoodDb.get(k)?.let { ShoppingItem(it, g) } }
            .sortedBy { it.food.category.ordinal }
    }
}
