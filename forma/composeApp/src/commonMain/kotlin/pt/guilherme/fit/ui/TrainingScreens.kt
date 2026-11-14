package pt.guilherme.fit.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import pt.guilherme.fit.AppController
import pt.guilherme.fit.data.Exercises
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.WorkoutGenerator
import pt.guilherme.fit.model.Experience
import pt.guilherme.fit.model.GymEquipment
import kotlin.math.roundToInt

@Composable
fun TrainingScreen(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    val profile = state.profile ?: return
    val today = c.today()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("Configuração") {
                Text("Dias por semana: ${profile.trainingDaysPerWeek}")
                Slider(
                    value = profile.trainingDaysPerWeek.toFloat(),
                    onValueChange = { v -> c.saveProfile(profile.copy(trainingDaysPerWeek = v.roundToInt())) },
                    valueRange = 1f..6f, steps = 4,
                )
                Text("Duração da sessão: ${profile.sessionMinutes} min")
                Slider(
                    value = profile.sessionMinutes.toFloat(),
                    onValueChange = { v -> c.saveProfile(profile.copy(sessionMinutes = (v / 5).roundToInt() * 5)) },
                    valueRange = 20f..120f,
                )
                Text("Experiência")
                ChoiceChips(Experience.entries.toList(), profile.experience, { it.label }) { c.saveProfile(profile.copy(experience = it)) }
                Text("Equipamento disponível")
                MultiChips(GymEquipment.entries.toList(), state.gymEquipment, { it.label }) { sel ->
                    c.store.update { it.copy(gymEquipment = sel + GymEquipment.BODYWEIGHT) }
                }
                Button(onClick = { c.generateWorkout() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.workoutPlan == null) "Gerar plano de treino" else "Gerar novo plano")
                }
                Hint("O plano tem em conta o teu objetivo (${profile.goal.label.lowercase()}): " +
                    "${WorkoutGenerator.cardioMinutes(profile.goal, profile.sessionMinutes)} min de cardio no fim de cada sessão.")
            }
        }

        val plan = state.workoutPlan
        if (plan != null) {
            item {
                Text("Plano: ${plan.splitName}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Hint("Sobe a carga quando fizeres o topo das repetições em todas as séries.")
            }
            items(plan.sessions) { s ->
                val nextDay = (0..6).map { today + it }.first { Dates.weekday(it) == s.weekday }
                SectionCard(
                    "${Dates.weekdaysLong[s.weekday - 1]} • ${s.name}",
                    action = {
                        FilledTonalButton(onClick = { nav.push(Route.Session(nextDay)) }) { Text(if (nextDay == today) "Hoje" else "Abrir") }
                    },
                ) {
                    s.exercises.forEach { pe ->
                        val ex = Exercises.get(pe.exerciseId) ?: return@forEach
                        Text("• ${ex.name}  —  ${pe.sets}×${pe.reps}")
                    }
                    if (s.cardioMinutes > 0) Text("• Cardio moderado  —  ${s.cardioMinutes} min")
                    Hint("~${WorkoutGenerator.estimateKcal(s, profile.weightKg, profile.sessionMinutes)} kcal estimadas")
                }
            }
        }

        item {
            val done = state.workoutLogs.filter { it.completed }.sortedByDescending { it.day }.take(10)
            SectionCard("Histórico") {
                if (done.isEmpty()) Hint("Ainda não concluíste nenhum treino.")
                done.forEach { Text("${Dates.short(it.day)} — ${it.sessionName}" + if (it.notes.isNotBlank()) " (${it.notes})" else "") }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
fun SessionScreen(c: AppController, day: Long) {
    val state by c.state.collectAsState()
    val session = c.sessionForDay(day, state)
    if (session == null) {
        Text("Não há treino planeado para este dia.", Modifier.padding(16.dp))
        return
    }
    val log = c.workoutLog(day, state)
    var notes by remember(log?.notes) { mutableStateOf(log?.notes ?: "") }
    var restLeft by remember { mutableStateOf(0) }
    LaunchedEffect(restLeft) {
        if (restLeft > 0) { delay(1000); restLeft -= 1 }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(session.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(Dates.long(day), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Hint("Aquece 5 min (mobilidade + 1 série leve do primeiro exercício).")
            if (restLeft > 0) {
                Text("Descanso: ${restLeft}s", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = { restLeft = 0 }) { Text("Saltar descanso") }
            }
        }
        items(session.exercises) { pe ->
            val ex = Exercises.get(pe.exerciseId) ?: return@items
            val checked = log?.doneExercises?.contains(pe.exerciseId) == true
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { c.toggleExercise(day, session.name, pe.exerciseId) }) {
                    Checkbox(checked = checked, onCheckedChange = { c.toggleExercise(day, session.name, pe.exerciseId) })
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontWeight = FontWeight.SemiBold)
                        Text("${pe.sets} séries × ${pe.reps} • descanso ${pe.restSec}s")
                        Text(ex.muscles.joinToString { it.label }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Hint(ex.cue)
                OutlinedButton(onClick = { restLeft = pe.restSec }) { Text("Iniciar descanso (${pe.restSec}s)") }
            }
        }
        if (session.cardioMinutes > 0) item {
            SectionCard("Cardio") {
                Text("${session.cardioMinutes} min a ritmo moderado (consegues falar, mas com esforço): passadeira inclinada, bicicleta, remo ou caminhada rápida.")
            }
        }
        item {
            OutlinedTextField(notes, { notes = it }, label = { Text("Notas (cargas, como correu)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { c.setWorkoutCompleted(day, session.name, !(log?.completed ?: false), notes) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (log?.completed == true) "Marcar como não feito" else "Concluir treino") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
