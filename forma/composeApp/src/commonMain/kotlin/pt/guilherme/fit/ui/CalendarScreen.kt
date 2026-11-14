package pt.guilherme.fit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.guilherme.fit.AppController
import pt.guilherme.fit.data.Exercises
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.fmt
import kotlin.math.roundToInt

@Composable
fun CalendarScreen(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    val today = c.today()
    val todayDate = Dates.fromEpochDay(today)
    var year by remember { mutableStateOf(todayDate.year) }
    var month by remember { mutableStateOf(todayDate.month) }
    val t = c.targets(state)

    val first = Dates.toEpochDay(year, month, 1)
    val offset = Dates.weekday(first) - 1
    val days = Dates.daysInMonth(year, month)

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (month == 1) { month = 12; year-- } else month-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mês anterior") }
                Text("${Dates.monthsLong[month - 1]} $year", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { if (month == 12) { month = 1; year++ } else month++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mês seguinte") }
            }
            Row(Modifier.fillMaxWidth()) {
                Dates.weekdaysShort.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            val cells = offset + days
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val idx = r * 7 + col
                        val dayNum = idx - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(0.85f).padding(2.dp)) {
                            if (dayNum in 1..days) {
                                val d = first + dayNum - 1
                                DayCell(c, d, dayNum, d == today, t?.kcal) { nav.push(Route.Day(d)) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Legend(FormaColors.workout, "Treino")
                Legend(MaterialTheme.colorScheme.primary, "Treino feito")
                Legend(FormaColors.carbs, "Refeições planeadas")
                Legend(FormaColors.protein, "Acima da meta")
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(" $label", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DayCell(c: AppController, day: Long, num: Int, isToday: Boolean, targetKcal: Double?, onClick: () -> Unit) {
    val state by c.state.collectAsState()
    val session = c.sessionForDay(day, state)
    val log = c.workoutLog(day, state)
    val hasMeals = state.mealPlan.any { it.day == day }
    val eaten = c.eaten(day, state).kcal
    val over = targetKcal != null && eaten > targetKcal * 1.05
    val border = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Text("$num", style = MaterialTheme.typography.labelMedium, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (log?.completed == true) Dot(MaterialTheme.colorScheme.primary)
            else if (session != null && (state.workoutPlan?.createdDay ?: Long.MAX_VALUE) <= day) Dot(FormaColors.workout)
            if (hasMeals) Dot(FormaColors.carbs)
            if (over) Dot(FormaColors.protein)
        }
        if (eaten > 0) Text("${eaten.roundToInt()}", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun Dot(color: Color) = Box(Modifier.size(6.dp).clip(CircleShape).background(color))

@Composable
fun DayScreen(c: AppController, day: Long, nav: Nav) {
    val state by c.state.collectAsState()
    val session = c.sessionForDay(day, state)
    val log = c.workoutLog(day, state)
    val eaten = c.eaten(day, state)
    val t = c.targets(state)
    val weight = state.weights.firstOrNull { it.day == day }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("Treino") {
                if (session == null) Text("Descanso")
                else {
                    Text(session.name + if (log?.completed == true) " ✓" else "")
                    session.exercises.forEach { pe -> Exercises.get(pe.exerciseId)?.let { Hint("• ${it.name} ${pe.sets}×${pe.reps}") } }
                    OutlinedButton(onClick = { nav.push(Route.Session(day)) }) { Text("Abrir treino") }
                }
            }
        }
        item {
            SectionCard("Comida") {
                MacroLine(eaten.kcal, eaten.protein, eaten.carbs, eaten.fat)
                t?.let { Hint("Meta ${it.kcal.roundToInt()} kcal") }
                Hint("Água: ${fmt(c.waterMl(day, state) / 1000.0, 2)} L" + (weight?.let { " • Peso: ${fmt(it.kg)} kg" } ?: ""))
                state.foods.filter { it.day == day }.sortedBy { it.slot.ordinal }.forEach {
                    Text("${it.slot.label}: ${it.name} — ${it.macros.kcal.roundToInt()} kcal")
                }
                Button(onClick = { nav.push(Route.AddFood(day, slotForHour(currentHour()))) }) { Text("Adicionar comida neste dia") }
            }
        }
        item {
            val planned = state.mealPlan.filter { it.day == day }.sortedBy { it.slot.ordinal }
            SectionCard("Refeições planeadas") {
                if (planned.isEmpty()) Hint("Sem plano para este dia.")
                planned.forEach { pm ->
                    val r = c.recipe(pm.recipeId) ?: return@forEach
                    val kcal = RecipeEngine.info(r).perServing.kcal * pm.servings
                    Text("${pm.slot.label}: ${r.name} (${kcal.roundToInt()} kcal)",
                        modifier = Modifier.clickable { nav.push(Route.RecipeDetail(r.id, day, pm.slot, pm.servings)) }.padding(vertical = 4.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
