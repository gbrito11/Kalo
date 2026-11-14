package pt.guilherme.fit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.guilherme.fit.AppController
import pt.guilherme.fit.data.FoodDb
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.MealPlanner
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.fmt
import pt.guilherme.fit.model.KitchenTool
import pt.guilherme.fit.model.MealSlot
import kotlin.math.roundToInt

@Composable
fun KitchenScreen(c: AppController, nav: Nav) {
    var tab by remember { mutableStateOf(0) }
    val tabs = listOf("Despensa", "Receitas", "Plano", "Compras")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        when (tab) {
            0 -> PantryTab(c)
            1 -> RecipesTab(c, nav)
            2 -> PlanTab(c, nav)
            else -> ShoppingTab(c)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PantryTab(c: AppController) {
    val state by c.state.collectAsState()
    var input by remember { mutableStateOf("") }
    val quick = listOf("Ovos", "Frango", "Arroz", "Massa", "Atum", "Grão", "Feijão", "Batata", "Cebola", "Tomate", "Iogurte grego", "Leite", "Aveia", "Banana", "Espinafres", "Brócolos", "Pão integral", "Queijo", "Salmão", "Lentilhas")
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("O que tens em casa") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(input, { input = it }, label = { Text("Ex.: frango, arroz, cenoura") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { c.addPantry(input); input = "" }, modifier = Modifier.padding(start = 8.dp)) { Text("Juntar") }
                }
                if (state.pantry.isEmpty()) Hint("Junta ingredientes para veres receitas que dás para fazer já.")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.pantry.forEach { item ->
                        InputChip(selected = false, onClick = { c.removePantry(item.id) }, label = { Text(item.name) },
                            trailingIcon = { Icon(Icons.Default.Close, "Remover") })
                    }
                }
                Hint("Sal, pimenta, azeite, alho, limão e especiarias contam sempre como existentes.")
            }
        }
        item {
            SectionCard("Junta rápido") {
                val have = state.pantry.map { it.name.lowercase() }.toSet()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.filter { it.lowercase() !in have }.forEach { q -> AssistChip(onClick = { c.addPantry(q) }, label = { Text("+ $q") }) }
                }
            }
        }
        item {
            SectionCard("Equipamento de cozinha") {
                MultiChips(KitchenTool.entries.toList(), state.kitchenTools, { it.label }) { sel -> c.store.update { it.copy(kitchenTools = sel) } }
            }
        }
    }
}

@Composable
private fun RecipesTab(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    val profile = state.profile ?: return
    var slot by remember { mutableStateOf<MealSlot?>(null) }
    var onlyQuick by remember { mutableStateOf(true) }
    val list = remember(state.pantry, state.kitchenTools, profile, slot, onlyQuick, state.customRecipes) {
        RecipeEngine.suggest(c.recipes, profile, state.pantry, state.kitchenTools, slot, respectTime = onlyQuick)
    }
    val rejected = remember(profile, state.kitchenTools) {
        c.recipes.size - RecipeEngine.suggest(c.recipes, profile, emptyList(), state.kitchenTools, null, respectTime = false).size
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            ChoiceChips(listOf<MealSlot?>(null) + MealSlot.entries, slot, { it?.label ?: "Todas" }) { slot = it }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(onlyQuick, { onlyQuick = it })
                Text("Só até ${profile.maxPrepMinutes} min")
            }
            Hint("Dieta ${profile.dietType.label.lowercase()}" +
                (if (profile.restrictions.isNotEmpty()) ", sem ${profile.restrictions.joinToString { it.label.lowercase() }}" else "") +
                (if (profile.dislikes.isNotEmpty()) ", sem ${profile.dislikes.joinToString()}" else "") +
                ". $rejected receitas escondidas por não encaixarem.")
        }
        if (list.isEmpty()) item { Hint("Nenhuma receita encaixa. Tira o filtro de tempo ou revê as preferências no perfil.") }
        items(list) { s ->
            Column(Modifier.fillMaxWidth().clickable { nav.push(Route.RecipeDetail(s.recipe.id)) }.padding(vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.recipe.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${(s.coverage * 100).roundToInt()}%", color = if (s.coverage >= 0.99) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Hint("${s.recipe.minutes} min • ${s.info.perServing.kcal.roundToInt()} kcal/dose • P ${s.info.perServing.protein.roundToInt()} g")
                if (s.missing.isNotEmpty()) Hint("Falta: ${s.missing.joinToString { it.name.lowercase() }}")
                else Text("Tens tudo!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalDivider()
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun RecipeDetailScreen(c: AppController, route: Route.RecipeDetail, onDone: () -> Unit) {
    val state by c.state.collectAsState()
    val r = c.recipe(route.id) ?: return
    val info = remember(r) { RecipeEngine.info(r) }
    val pantryNorm = remember(state.pantry) { RecipeEngine.normPantry(state.pantry) }
    var servings by remember { mutableStateOf(route.servings ?: 1.0) }
    var slot by remember { mutableStateOf(route.slot ?: r.slots.first()) }
    val day = route.day ?: c.today()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Hint("${r.minutes} min • receita para ${r.servings} dose(s)" + if (r.mediterranean) " • mediterrânica" else "")
            val m = info.perServing * servings
            Text("Por ${fmt(servings)} dose(s):", style = MaterialTheme.typography.labelLarge)
            MacroLine(m.kcal, m.protein, m.carbs, m.fat)
            val tags = buildList {
                if (info.vegan) add("vegan") else if (info.vegetarian) add("vegetariana")
                if (info.allergens.isNotEmpty()) add("contém: " + info.allergens.joinToString { it.label.lowercase() })
            }
            if (tags.isNotEmpty()) Hint(tags.joinToString(" • "))
        }
        item {
            SectionCard("Ingredientes (para ${fmt(servings)} dose(s))") {
                val factor = servings / r.servings
                r.ingredients.forEach { ing ->
                    val f = FoodDb.get(ing.foodKey) ?: return@forEach
                    val have = RecipeEngine.pantryHas(f, pantryNorm)
                    val qty = ing.grams * factor
                    val unitTxt = f.unitGrams?.let { u -> if (qty >= u * 0.75) " (~${fmt(qty / u)} ${f.unitName})" else "" } ?: ""
                    Row {
                        Text(if (have) "✓ " else "• ", color = if (have) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${f.name} — ${qty.roundToInt()} g$unitTxt" + if (ing.optional) " (opcional)" else "")
                    }
                }
            }
        }
        item {
            SectionCard("Preparação") {
                r.steps.forEachIndexed { i, s -> Text("${i + 1}. $s") }
            }
        }
        item {
            SectionCard("Registar no diário") {
                ChoiceChips(MealSlot.entries.toList(), slot, { it.label }) { slot = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Doses: ")
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { s ->
                        TextButton(onClick = { servings = s }) { Text(if (s == servings) "[${fmt(s)}]" else fmt(s)) }
                    }
                }
                Button(onClick = { c.logRecipe(r, servings, day, slot); onDone() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Registar em ${if (day == c.today()) "hoje" else Dates.short(day)}")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlanTab(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    val today = c.today()
    val upcoming = state.mealPlan.filter { it.day >= today }.groupBy { it.day }.toList().sortedBy { it.first }
    val t = c.targets(state)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { c.generateMealPlan(7) }) { Text(if (upcoming.isEmpty()) "Gerar plano da semana" else "Gerar outro plano") }
                if (upcoming.isNotEmpty()) OutlinedButton(onClick = { c.clearMealPlan() }) { Text("Limpar") }
            }
            Hint("Usa a tua meta (${t?.kcal?.roundToInt() ?: "–"} kcal), a dieta, as restrições, o tempo máximo e dá prioridade ao que tens na despensa.")
        }
        upcoming.forEach { (day, meals) ->
            item {
                val total = meals.sumOf { pm -> c.recipe(pm.recipeId)?.let { RecipeEngine.info(it).perServing.kcal * pm.servings } ?: 0.0 }
                SectionCard("${Dates.long(day)} • ${total.roundToInt()} kcal") {
                    meals.sortedBy { it.slot.ordinal }.forEach { pm ->
                        val r = c.recipe(pm.recipeId) ?: return@forEach
                        val logged = state.foods.any { it.day == day && it.slot == pm.slot && it.name.startsWith(r.name) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).clickable { nav.push(Route.RecipeDetail(r.id, day, pm.slot, pm.servings)) }) {
                                Text(pm.slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text("${r.name} ×${fmt(pm.servings)}")
                            }
                            if (logged) Text("✓", color = MaterialTheme.colorScheme.primary)
                            else if (day == today) TextButton(onClick = { c.logPlannedMeal(pm) }) { Text("Registar") }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ShoppingTab(c: AppController) {
    val state by c.state.collectAsState()
    val today = c.today()
    val plan = state.mealPlan.filter { it.day >= today }
    val list = remember(plan, state.pantry) { MealPlanner.shoppingList(plan, c.recipes, state.pantry) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (plan.isEmpty()) item { Hint("Gera primeiro um plano no separador Plano.") }
        else if (list.isEmpty()) item { Hint("Já tens tudo o que o plano precisa.") }
        else item { Hint("Toca num item quando o comprares: passa para a despensa.") }
        items(list) { item ->
            Row(Modifier.fillMaxWidth().clickable { c.addPantry(item.food.name) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.food.name, modifier = Modifier.weight(1f))
                val units = item.food.unitGrams?.let { u -> " (~${kotlin.math.ceil(item.grams / u).toInt()} ${item.food.unitName})" } ?: ""
                Text("${(kotlin.math.ceil(item.grams / 10) * 10).toInt()} g$units", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
        }
    }
}
