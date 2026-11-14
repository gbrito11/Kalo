package pt.guilherme.fit

import pt.guilherme.fit.data.Exercises
import pt.guilherme.fit.data.FoodDb
import pt.guilherme.fit.data.Food101
import pt.guilherme.fit.data.builtInRecipes
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.MealPlanner
import pt.guilherme.fit.domain.Nutrition
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.domain.WorkoutGenerator
import pt.guilherme.fit.model.DietType
import pt.guilherme.fit.model.Experience
import pt.guilherme.fit.model.Goal
import pt.guilherme.fit.model.GymEquipment
import pt.guilherme.fit.model.KitchenTool
import pt.guilherme.fit.model.PantryItem
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Restriction
import pt.guilherme.fit.model.Sex
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DomainTest {

    private val me = Profile(
        sex = Sex.MALE, age = 21, heightCm = 178.0, weightKg = 80.0, targetWeightKg = 74.0,
        goal = Goal.LOSE, weeklyRateKg = 0.5, dietType = DietType.MEDITERRANEAN,
    )
    private val allTools = KitchenTool.entries.toSet()

    @Test
    fun datesRoundTrip() {
        assertEquals(0L, Dates.toEpochDay(1970, 1, 1))
        assertEquals(4, Dates.weekday(0)) // quinta-feira
        val d = Dates.toEpochDay(2026, 11, 13)
        assertEquals(5, Dates.weekday(d)) // sexta-feira
        val back = Dates.fromEpochDay(d)
        assertEquals(2026, back.year); assertEquals(11, back.month); assertEquals(13, back.day)
        assertEquals(29, Dates.daysInMonth(2028, 2))
        assertEquals(28, Dates.daysInMonth(2026, 2))
        for (x in -1000L..100000L step 37) {
            val s = Dates.fromEpochDay(x)
            assertEquals(x, Dates.toEpochDay(s.year, s.month, s.day))
        }
    }

    @Test
    fun mifflinAndDeficit() {
        // 10*80 + 6.25*178 - 5*21 + 5 = 1812.5
        assertTrue(abs(Nutrition.bmr(me) - 1812.5) < 0.01)
        val t = Nutrition.targets(me)
        val expectedTdee = 1812.5 * 1.375
        assertTrue(abs(t.tdee - expectedTdee) < 0.01)
        assertTrue(abs(t.dailyDelta + 550.0) < 0.01, "0,5 kg/semana = 550 kcal/dia")
        assertTrue(abs(t.protein - 160.0) < 0.01)
        assertTrue(abs((t.protein * 4 + t.carbs * 4 + t.fat * 9) - t.kcal) < 1.0, "macros somam as kcal")
        val weeks = assertNotNull(t.weeksToGoal)
        assertTrue(weeks in 11.0..13.0)
    }

    @Test
    fun deficitNeverBelowFloor() {
        val small = me.copy(sex = Sex.FEMALE, weightKg = 50.0, heightCm = 155.0, targetWeightKg = 45.0, weeklyRateKg = 1.0, activity = pt.guilherme.fit.model.ActivityLevel.SEDENTARY)
        val t = Nutrition.targets(small)
        assertTrue(t.kcal >= 1200.0)
    }

    @Test
    fun ketoCapsCarbs() {
        val t = Nutrition.targets(me.copy(dietType = DietType.KETO))
        assertTrue(t.carbs <= 30.0)
        assertTrue(t.fat > 100)
    }

    @Test
    fun allRecipeIngredientsExist() {
        for (r in builtInRecipes) for (i in r.ingredients) {
            assertNotNull(FoodDb.get(i.foodKey), "ingrediente ${i.foodKey} da receita ${r.id}")
        }
        assertEquals(builtInRecipes.size, builtInRecipes.map { it.id }.toSet().size, "ids únicos")
    }

    @Test
    fun recipeFilters() {
        val vegan = me.copy(dietType = DietType.VEGAN)
        val sug = RecipeEngine.suggest(builtInRecipes, vegan, emptyList(), allTools)
        assertTrue(sug.isNotEmpty())
        assertTrue(sug.all { it.info.vegan })

        val noFish = me.copy(dietType = DietType.BALANCED, restrictions = setOf(Restriction.FISH))
        assertTrue(RecipeEngine.suggest(builtInRecipes, noFish, emptyList(), allTools).none { Restriction.FISH in it.info.allergens })

        val hatesTuna = me.copy(dietType = DietType.BALANCED, dislikes = listOf("Atum"))
        assertTrue(RecipeEngine.suggest(builtInRecipes, hatesTuna, emptyList(), allTools).none { s -> s.recipe.ingredients.any { it.foodKey == "atum" } })

        val quick = me.copy(dietType = DietType.BALANCED, maxPrepMinutes = 10)
        assertTrue(RecipeEngine.suggest(builtInRecipes, quick, emptyList(), allTools).all { it.recipe.minutes <= 10 })

        val onlyMicro = RecipeEngine.suggest(builtInRecipes, me.copy(dietType = DietType.BALANCED), emptyList(), setOf(KitchenTool.MICROWAVE))
        assertTrue(onlyMicro.all { it.recipe.allTools.isEmpty() && (it.recipe.anyTools.isEmpty() || KitchenTool.MICROWAVE in it.recipe.anyTools) })

        val keto = RecipeEngine.suggest(builtInRecipes, me.copy(dietType = DietType.KETO), emptyList(), allTools)
        assertTrue(keto.isNotEmpty(), "há receitas cetogénicas")
    }

    @Test
    fun pantryMatching() {
        val pantry = listOf("Frango", "arroz", "Brócolos").map { PantryItem(it, it) }
        val sug = RecipeEngine.suggest(builtInRecipes, me.copy(dietType = DietType.BALANCED), pantry, allTools)
        assertEquals("frango_arroz_brocolos", sug.first().recipe.id)
        assertEquals(1.0, sug.first().coverage)
    }

    @Test
    fun mealPlanHitsCalories() {
        val start = Dates.toEpochDay(2026, 11, 16)
        val t = Nutrition.targets(me)
        val plan = MealPlanner.plan(start, 7, me, t.kcal, builtInRecipes, emptyList(), allTools)
        assertEquals(28, plan.size)
        val byDay = plan.groupBy { it.day }
        for ((_, meals) in byDay) {
            val kcal = meals.sumOf { pm -> RecipeEngine.info(builtInRecipes.first { it.id == pm.recipeId }).perServing.kcal * pm.servings }
            assertTrue(kcal in t.kcal * 0.7..t.kcal * 1.3, "dia com $kcal kcal vs alvo ${t.kcal}")
        }
        val list = MealPlanner.shoppingList(plan, builtInRecipes, emptyList())
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun workoutPlans() {
        for (days in 1..6) for (eq in listOf(setOf(GymEquipment.BODYWEIGHT), setOf(GymEquipment.DUMBBELLS, GymEquipment.BENCH), GymEquipment.entries.toSet())) {
            val plan = WorkoutGenerator.generate(me.copy(trainingDaysPerWeek = days, experience = Experience.INTERMEDIATE), eq, 0)
            assertEquals(days, plan.sessions.size)
            assertEquals(days, plan.sessions.map { it.weekday }.toSet().size)
            for (s in plan.sessions) {
                assertTrue(s.exercises.size >= 3, "${plan.splitName}/${s.name} com ${eq}")
                for (pe in s.exercises) {
                    val ex = assertNotNull(Exercises.get(pe.exerciseId))
                    assertTrue((eq + GymEquipment.BODYWEIGHT).containsAll(ex.equipment))
                }
            }
        }
    }

    @Test
    fun food101TableComplete() {
        assertEquals(101, Food101.labels.size)
        for (l in Food101.labels) assertNotNull(Food101.get(l), l)
    }
}
