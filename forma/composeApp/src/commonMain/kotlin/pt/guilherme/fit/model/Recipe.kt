package pt.guilherme.fit.model

import kotlinx.serialization.Serializable

@Serializable
enum class FoodCategory {
    MEAT, POULTRY, PROCESSED_MEAT, FISH, SEAFOOD, DAIRY, EGG, GRAIN, TUBER, LEGUME, PLANT_PROTEIN,
    VEGETABLE, FRUIT, NUT_SEED, FAT, SWEET, CONDIMENT, SUPPLEMENT,
}

/** Alimento base com valores por 100 g (valores aproximados de tabelas nutricionais). */
@Serializable
data class Food(
    val key: String,
    val name: String,
    val per100: Macros,
    val category: FoodCategory,
    val allergens: Set<Restriction> = emptySet(),
    val synonyms: List<String> = emptyList(),
    /** ingredientes que se assume haver sempre em casa (sal, azeite, especiarias...) */
    val staple: Boolean = false,
    /** gramas de uma unidade (1 ovo, 1 banana...) */
    val unitGrams: Double? = null,
    val unitName: String? = null,
    val vegan: Boolean = category in setOf(
        FoodCategory.GRAIN, FoodCategory.TUBER, FoodCategory.LEGUME, FoodCategory.PLANT_PROTEIN,
        FoodCategory.VEGETABLE, FoodCategory.FRUIT, FoodCategory.NUT_SEED, FoodCategory.FAT,
        FoodCategory.CONDIMENT,
    ),
)

@Serializable
data class RecipeIngredient(
    val foodKey: String,
    /** gramas para a receita inteira */
    val grams: Double,
    val optional: Boolean = false,
)

@Serializable
data class Recipe(
    val id: String,
    val name: String,
    val slots: Set<MealSlot>,
    val minutes: Int,
    val servings: Int,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
    /** precisa de pelo menos um destes (vazio = não precisa de nada) */
    val anyTools: Set<KitchenTool> = emptySet(),
    /** precisa de todos estes */
    val allTools: Set<KitchenTool> = emptySet(),
    val mediterranean: Boolean = false,
)
