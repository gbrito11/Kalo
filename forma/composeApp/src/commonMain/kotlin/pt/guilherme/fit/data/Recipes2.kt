package pt.guilherme.fit.data

import pt.guilherme.fit.model.KitchenTool
import pt.guilherme.fit.model.MealSlot
import pt.guilherme.fit.model.Recipe
import pt.guilherme.fit.model.RecipeIngredient

/** Receitas extra pensadas para low carb / cetogénica. */
internal object LowCarbRecipes {
    val all = listOf(
        Recipe(
            id = "salmao_espinafres_abacate",
            name = "Salmão grelhado com espinafres salteados e abacate",
            slots = setOf(MealSlot.LUNCH, MealSlot.DINNER),
            minutes = 20, servings = 2,
            ingredients = listOf(
                RecipeIngredient("salmao", 300.0), RecipeIngredient("espinafres", 200.0),
                RecipeIngredient("abacate", 150.0), RecipeIngredient("azeite", 15.0),
                RecipeIngredient("limao", 10.0), RecipeIngredient("alho", 5.0),
            ),
            steps = listOf(
                "Grelha o salmão 4 min de cada lado.",
                "Salteia os espinafres com o alho no azeite.",
                "Serve com o abacate às fatias e limão.",
            ),
            anyTools = setOf(KitchenTool.STOVE, KitchenTool.GRILL_PAN),
            mediterranean = true,
        ),
        Recipe(
            id = "omelete_queijo_cogumelos",
            name = "Omelete de queijo e cogumelos",
            slots = setOf(MealSlot.BREAKFAST, MealSlot.DINNER),
            minutes = 12, servings = 1,
            ingredients = listOf(
                RecipeIngredient("ovo", 165.0), RecipeIngredient("queijo", 30.0),
                RecipeIngredient("cogumelos", 80.0), RecipeIngredient("manteiga", 10.0),
                RecipeIngredient("espinafres", 40.0),
            ),
            steps = listOf(
                "Salteia os cogumelos na manteiga 4 min.",
                "Junta os ovos batidos e os espinafres.",
                "Quando estiver quase pronta, junta o queijo e dobra.",
            ),
            anyTools = setOf(KitchenTool.STOVE),
        ),
    )
}

/** Todas as receitas da app (base + extra). */
val builtInRecipes: List<Recipe> = Recipes.all + LowCarbRecipes.all
