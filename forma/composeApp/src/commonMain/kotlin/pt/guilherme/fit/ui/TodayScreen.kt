package pt.guilherme.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.guilherme.fit.AppController
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.Nutrition
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.domain.WorkoutGenerator
import pt.guilherme.fit.fmt
import kotlin.math.roundToInt

@Composable
fun TodayScreen(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    val health by c.health.collectAsState()
    val healthStatus by c.healthStatus.collectAsState()
    val profile = state.profile ?: return
    val today = c.today()
    val t = c.targets(state) ?: return
    val eaten = c.eaten(today, state)
    val water = c.waterMl(today, state)
    val scope = rememberCoroutineScope()
    var coachText by remember { mutableStateOf<String?>(null) }
    var coachLoading by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(Dates.long(today), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (profile.name.isNotBlank()) Text("Olá, ${profile.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        // Calorias e macros
        item {
            SectionCard("Calorias") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CalorieRing(eaten.kcal, t.kcal)
                    Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroBar("Proteína", eaten.protein, t.protein, FormaColors.protein)
                        MacroBar("Hidratos", eaten.carbs, t.carbs, FormaColors.carbs)
                        MacroBar("Gordura", eaten.fat, t.fat, FormaColors.fat)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Meta", "${t.kcal.roundToInt()}")
                    Stat("Comido", "${eaten.kcal.roundToInt()}")
                    Stat(if (t.dailyDelta < 0) "Défice" else "Excedente", "${kotlin.math.abs(t.dailyDelta).roundToInt()}")
                    Stat("Gasto", "${t.tdee.roundToInt()}")
                }
                t.weeksToGoal?.let { Hint("Ao ritmo atual chegas aos ${fmt(profile.targetWeightKg)} kg em cerca de ${Nutrition.formatWeeks(it)}.") }
            }
        }

        // Água
        item {
            SectionCard("Água", action = { TextButton(onClick = { c.undoWater() }) { Text("Desfazer") } }) {
                MacroBar("${fmt(water / 1000.0, 2)} L de ${fmt(t.waterMl / 1000.0, 1)} L", water.toDouble(), t.waterMl.toDouble(), FormaColors.water, "ml")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(250, 330, 500).forEach { ml ->
                        FilledTonalButton(onClick = { c.addWater(ml) }) {
                            Icon(Icons.Default.WaterDrop, null, Modifier.size(16.dp)); Text(" +$ml")
                        }
                    }
                }
            }
        }

        // Treino de hoje
        item {
            val session = c.sessionForDay(today, state)
            val log = c.workoutLog(today, state)
            SectionCard("Treino de hoje") {
                if (state.workoutPlan == null) {
                    Hint("Ainda não tens plano de treino.")
                    OutlinedButton(onClick = { c.generateWorkout() }) { Text("Gerar plano de treino") }
                } else if (session == null) {
                    Text("Dia de descanso. Uma caminhada ajuda na recuperação.")
                } else {
                    Text(session.name, style = MaterialTheme.typography.titleMedium)
                    Hint("${session.exercises.size} exercícios" + (if (session.cardioMinutes > 0) " + ${session.cardioMinutes} min de cardio" else "") +
                        " • ~${WorkoutGenerator.estimateKcal(session, profile.weightKg, profile.sessionMinutes)} kcal")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { nav.push(Route.Session(today)) }) { Text(if (log?.completed == true) "Ver treino ✓" else "Começar treino") }
                        if (log != null && !log.completed) Hint("  ${log.doneExercises.size}/${session.exercises.size} feitos")
                    }
                }
            }
        }

        // Saúde
        item {
            SectionCard(c.healthName, action = {
                IconButton(onClick = { c.refreshHealth() }) { Icon(Icons.Default.Refresh, "Atualizar") }
            }) {
                if (health.steps == null && health.activeKcal == null) {
                    Hint("Liga a app ao ${c.healthName} para ler passos, calorias ativas, minutos de exercício e peso.")
                    OutlinedButton(onClick = { c.connectHealth() }) {
                        Icon(Icons.Default.Favorite, null, Modifier.size(16.dp)); Text("  Ligar ao ${c.healthName}")
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stat("Passos", health.steps?.toString() ?: "–")
                        Stat("Kcal ativas", health.activeKcal?.roundToInt()?.toString() ?: "–")
                        Stat("Min. exercício", health.exerciseMinutes?.roundToInt()?.toString() ?: "–")
                    }
                    if (!state.settings.useHealthActiveEnergy && health.activeKcal != null) {
                        Hint("Podes usar as kcal ativas reais no cálculo da meta (Perfil → Definições).")
                    }
                }
                healthStatus?.let { Hint(it) }
            }
        }

        // Peso
        item {
            var newWeight by remember { mutableStateOf<Double?>(null) }
            SectionCard("Peso") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Atual", "${fmt(profile.weightKg)} kg")
                    Stat("Objetivo", "${fmt(profile.targetWeightKg)} kg")
                    Stat("IMC", fmt(Nutrition.bmi(profile)))
                }
                LineChart(state.weights.takeLast(30).map { it.kg }, goal = profile.targetWeightKg)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NumberField("Peso de hoje", null, { newWeight = it }, Modifier.weight(1f), suffix = "kg")
                    Button(onClick = { newWeight?.let { if (it in 30.0..300.0) c.logWeight(it) } }, modifier = Modifier.padding(start = 8.dp)) { Text("Guardar") }
                }
            }
        }

        // Refeições planeadas para hoje
        item {
            val planned = state.mealPlan.filter { it.day == today }.sortedBy { it.slot.ordinal }
            SectionCard("Refeições planeadas") {
                if (planned.isEmpty()) Hint("Gera um plano semanal no separador Cozinha.")
                planned.forEach { pm ->
                    val r = c.recipe(pm.recipeId) ?: return@forEach
                    val kcal = RecipeEngine.info(r).perServing.kcal * pm.servings
                    val logged = state.foods.any { it.day == today && it.slot == pm.slot && it.name.startsWith(r.name) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(pm.slot.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { nav.push(Route.RecipeDetail(r.id, today, pm.slot, pm.servings)) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text("${r.name} • ${kcal.roundToInt()} kcal")
                            }
                        }
                        if (logged) Text("✓", color = MaterialTheme.colorScheme.primary)
                        else TextButton(onClick = { c.logPlannedMeal(pm) }) { Text("Registar") }
                    }
                }
            }
        }

        // Coach IA
        item {
            SectionCard("Dica do coach (Gemini)") {
                when {
                    coachLoading -> CircularProgressIndicator(Modifier.size(24.dp))
                    coachText != null -> Text(coachText!!)
                    else -> Hint("Analisa o teu dia (comida, água, treino e dados de saúde) e dá-te 2-3 ações concretas.")
                }
                OutlinedButton(enabled = !coachLoading, onClick = {
                    coachLoading = true
                    scope.launch {
                        coachText = try { c.askCoach() } catch (e: Exception) { e.message ?: "Erro" }
                        coachLoading = false
                    }
                }) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp)); Text("  Pedir dica")
                }
            }
        }
        item { androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp)) }
    }
}
