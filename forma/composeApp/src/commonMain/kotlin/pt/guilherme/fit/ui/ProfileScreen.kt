package pt.guilherme.fit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pt.guilherme.fit.AppController
import pt.guilherme.fit.domain.Nutrition
import pt.guilherme.fit.fmt
import pt.guilherme.fit.model.ActivityLevel
import pt.guilherme.fit.model.AnalyzerMode
import pt.guilherme.fit.model.DietType
import pt.guilherme.fit.model.Goal
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Restriction
import pt.guilherme.fit.model.Sex
import kotlin.math.roundToInt

@Composable
fun ProfileScreen(c: AppController, onboarding: Boolean, onDone: () -> Unit) {
    val state by c.state.collectAsState()
    var p by remember { mutableStateOf(state.profile ?: Profile()) }
    var dislikesText by remember { mutableStateOf(p.dislikes.joinToString(", ")) }
    val settings = state.settings
    val preview = remember(p) { Nutrition.targets(p) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onboarding) item {
            Spacer(Modifier.height(32.dp))
            Text("Bem-vindo ao Forma", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Preenche os teus dados para calcular calorias, macros, plano de refeições e treino.")
        }

        item {
            SectionCard("Sobre ti") {
                OutlinedTextField(p.name, { p = p.copy(name = it) }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ChoiceChips(Sex.entries.toList(), p.sex, { it.label }) { p = p.copy(sex = it) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Idade", p.age.toDouble(), { it?.let { v -> p = p.copy(age = v.roundToInt().coerceIn(14, 100)) } }, Modifier.weight(1f), decimals = 0)
                    NumberField("Altura", p.heightCm, { it?.let { v -> p = p.copy(heightCm = v) } }, Modifier.weight(1f), suffix = "cm", decimals = 0)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Peso atual", p.weightKg, { it?.let { v -> p = p.copy(weightKg = v) } }, Modifier.weight(1f), suffix = "kg")
                    NumberField("Peso objetivo", p.targetWeightKg, { it?.let { v -> p = p.copy(targetWeightKg = v) } }, Modifier.weight(1f), suffix = "kg")
                }
            }
        }

        item {
            SectionCard("Objetivo e atividade") {
                ChoiceChips(Goal.entries.toList(), p.goal, { it.label }) { p = p.copy(goal = it) }
                if (p.goal != Goal.MAINTAIN) {
                    Text("Ritmo: ${fmt(p.weeklyRateKg, 2)} kg por semana")
                    Slider(p.weeklyRateKg.toFloat(), { p = p.copy(weeklyRateKg = ((it * 4).roundToInt() / 4.0).coerceAtLeast(0.25)) }, valueRange = 0.25f..1f, steps = 2)
                    Hint(if (p.goal == Goal.LOSE) "0,5 kg/semana é um bom equilíbrio entre resultados e manter músculo." else "0,25 kg/semana minimiza o ganho de gordura.")
                }
                Text("Nível de atividade (sem contar com o treino da app se usares os dados de saúde):")
                ActivityLevel.entries.forEach { a ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(selected = p.activity == a, onClick = { p = p.copy(activity = a) })
                        Column { Text(a.label); Hint(a.hint) }
                    }
                }
            }
        }

        item {
            SectionCard("Alimentação") {
                Text("Tipo de dieta")
                ChoiceChips(DietType.entries.toList(), p.dietType, { it.label }) { p = p.copy(dietType = it) }
                Text("Alergias / intolerâncias")
                MultiChips(Restriction.entries.toList(), p.restrictions, { it.label }) { p = p.copy(restrictions = it) }
                OutlinedTextField(
                    dislikesText,
                    { dislikesText = it; p = p.copy(dislikes = it.split(',').map { s -> s.trim() }.filter { s -> s.isNotEmpty() }) },
                    label = { Text("Não gosto de… (separado por vírgulas)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Tempo máximo por refeição: ${p.maxPrepMinutes} min")
                Slider(p.maxPrepMinutes.toFloat(), { p = p.copy(maxPrepMinutes = (it / 5).roundToInt() * 5) }, valueRange = 5f..60f)
                Text("Refeições por dia: ${p.mealsPerDay}")
                Slider(p.mealsPerDay.toFloat(), { p = p.copy(mealsPerDay = it.roundToInt()) }, valueRange = 2f..4f, steps = 1)
            }
        }

        item {
            SectionCard("As tuas metas") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Basal", "${preview.bmr.roundToInt()}")
                    Stat("Gasto", "${preview.tdee.roundToInt()}")
                    Stat("Meta", "${preview.kcal.roundToInt()} kcal")
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("Proteína", "${preview.protein.roundToInt()} g")
                    Stat("Hidratos", "${preview.carbs.roundToInt()} g")
                    Stat("Gordura", "${preview.fat.roundToInt()} g")
                    Stat("Água", "${fmt(preview.waterMl / 1000.0)} L")
                }
                preview.weeksToGoal?.let { Hint("Tempo estimado até ao objetivo: ${Nutrition.formatWeeks(it)}.") }
                preview.notes.forEach { Hint(it) }
                Hint("Fórmula de Mifflin-St Jeor. É uma estimativa: ajusta pela evolução real do peso ao fim de 2-3 semanas.")
            }
        }

        item {
            Button(onClick = { c.saveProfile(p); onDone() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (onboarding) "Começar" else "Guardar perfil")
            }
        }

        if (!onboarding) {
            item {
                SectionCard("Análise de fotos") {
                    AnalyzerMode.entries.forEach { m ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.RadioButton(selected = settings.analyzerMode == m, onClick = { c.updateSettings { it.copy(analyzerMode = m) } })
                            Text(m.label)
                        }
                    }
                    Hint("Modelo local: ${if (c.services.classifier.isAvailable) "instalado ✓" else "não instalado (vê ml/README.md)"}")
                    var key by remember { mutableStateOf(settings.geminiApiKey) }
                    OutlinedTextField(
                        key, { key = it; c.updateSettings { s -> s.copy(geminiApiKey = it.trim()) } },
                        label = { Text("Chave API do Gemini") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
                    )
                    Hint("Gratuita em aistudio.google.com → Get API key. Fica guardada só neste telemóvel.")
                    var model by remember { mutableStateOf(settings.geminiModel) }
                    OutlinedTextField(model, { model = it; c.updateSettings { s -> s.copy(geminiModel = it.trim()) } }, label = { Text("Modelo Gemini") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Usar o Gemini quando a certeza local for menor que ${(settings.localConfidenceThreshold * 100).roundToInt()}%")
                    Slider(settings.localConfidenceThreshold.toFloat(), { v -> c.updateSettings { it.copy(localConfidenceThreshold = (v * 20).roundToInt() / 20.0) } }, valueRange = 0.2f..0.9f)
                }
            }
            item {
                SectionCard(c.healthName) {
                    SwitchRow("Usar kcal ativas reais no cálculo da meta", settings.useHealthActiveEnergy) { v -> c.updateSettings { it.copy(useHealthActiveEnergy = v) } }
                    Hint("Se ligares isto, escolhe \"Sedentário\" no nível de atividade para não contar o exercício duas vezes.")
                    SwitchRow("Enviar água", settings.syncWaterToHealth) { v -> c.updateSettings { it.copy(syncWaterToHealth = v) } }
                    SwitchRow("Enviar peso", settings.syncWeightToHealth) { v -> c.updateSettings { it.copy(syncWeightToHealth = v) } }
                    SwitchRow("Enviar calorias e macros", settings.syncFoodToHealth) { v -> c.updateSettings { it.copy(syncFoodToHealth = v) } }
                    Button(onClick = { c.connectHealth() }) { Text("Ligar / pedir permissões") }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked, onChange)
    }
}
