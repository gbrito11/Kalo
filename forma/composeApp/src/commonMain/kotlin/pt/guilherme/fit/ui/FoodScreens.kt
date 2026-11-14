package pt.guilherme.fit.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.guilherme.fit.AppController
import pt.guilherme.fit.ai.FoodGuess
import pt.guilherme.fit.data.AppStore
import pt.guilherme.fit.data.FoodDb
import pt.guilherme.fit.domain.Dates
import pt.guilherme.fit.domain.RecipeEngine
import pt.guilherme.fit.fmt
import pt.guilherme.fit.model.EntrySource
import pt.guilherme.fit.model.Food
import pt.guilherme.fit.model.FoodEntry
import pt.guilherme.fit.model.Macros
import pt.guilherme.fit.model.MealSlot
import pt.guilherme.fit.platform.decodeImage
import pt.guilherme.fit.platform.rememberImagePicker
import kotlin.math.roundToInt

/** Refeição mais provável pela hora do dia. */
fun slotForHour(hour: Int): MealSlot = when (hour) {
    in 5..10 -> MealSlot.BREAKFAST
    in 11..14 -> MealSlot.LUNCH
    in 15..18 -> MealSlot.SNACK
    else -> MealSlot.DINNER
}

fun currentHour(): Int = pt.guilherme.fit.platform.localHour()

// ---------------------------------------------------------------- Diário

@Composable
fun DiaryScreen(c: AppController, nav: Nav) {
    val state by c.state.collectAsState()
    var day by remember { mutableStateOf(c.today()) }
    val t = c.targets(state)
    val eaten = c.eaten(day, state)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { day-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Dia anterior") }
                    Text(
                        if (day == c.today()) "Hoje" else Dates.long(day),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { day++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Dia seguinte") }
                }
                if (t != null) {
                    MacroLine(eaten.kcal, eaten.protein, eaten.carbs, eaten.fat)
                    Hint("Meta: ${t.kcal.roundToInt()} kcal • P ${t.protein.roundToInt()} g • H ${t.carbs.roundToInt()} g • G ${t.fat.roundToInt()} g")
                }
            }
            items(MealSlot.entries.toList()) { slot ->
                val entries = state.foods.filter { it.day == day && it.slot == slot }
                val sum = entries.fold(Macros()) { a, e -> a + e.macros }
                SectionCard(
                    "${slot.label} • ${sum.kcal.roundToInt()} kcal",
                    action = {
                        IconButton(onClick = { nav.push(Route.AddFood(day, slot, startWithPhoto = true)) }) { Icon(Icons.Default.CameraAlt, "Foto") }
                        IconButton(onClick = { nav.push(Route.AddFood(day, slot)) }) { Icon(Icons.Default.Add, "Adicionar") }
                    },
                ) {
                    if (entries.isEmpty()) Hint("Nada registado.")
                    entries.forEach { e ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name + (e.grams?.let { " (${it.roundToInt()} g)" } ?: ""))
                                MacroLine(e.macros.kcal, e.macros.protein, e.macros.carbs, e.macros.fat)
                            }
                            IconButton(onClick = { c.removeFood(e.id) }) { Icon(Icons.Default.Delete, "Apagar") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { nav.push(Route.AddFood(day, slotForHour(currentHour()), startWithPhoto = true)) },
            icon = { Icon(Icons.Default.CameraAlt, null) },
            text = { Text("Foto da refeição") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

// ---------------------------------------------------------------- Adicionar comida

@Composable
fun AddFoodScreen(c: AppController, day: Long, initialSlot: MealSlot, startWithPhoto: Boolean, onDone: () -> Unit) {
    var slot by remember { mutableStateOf(initialSlot) }
    var tab by remember { mutableStateOf(if (startWithPhoto) 0 else 1) }
    val tabs = listOf("Foto", "Pesquisar", "Receita", "Manual")

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ChoiceChips(MealSlot.entries.toList(), slot, { it.label }) { slot = it }
        }
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, name -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(name) }) }
        }
        Box(Modifier.weight(1f).padding(16.dp)) {
            when (tab) {
                0 -> PhotoTab(c, day, slot, autoOpenCamera = startWithPhoto, onDone = onDone)
                1 -> SearchTab(c, day, slot, onDone)
                2 -> RecipeTab(c, day, slot, onDone)
                else -> ManualTab(c, day, slot, onDone)
            }
        }
    }
}

private class EditableGuess(guess: FoodGuess) {
    var name by mutableStateOf(guess.name)
    var baseGrams by mutableStateOf(guess.grams)
    var base by mutableStateOf(guess.macros)
    var factor by mutableStateOf(1f)
    var alternatives by mutableStateOf(guess.alternatives)
    val source = guess.source
    val confidence = guess.confidence
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PhotoTab(c: AppController, day: Long, slot: MealSlot, autoOpenCamera: Boolean, onDone: () -> Unit) {
    val state by c.state.collectAsState()
    val scope = rememberCoroutineScope()
    var image by remember { mutableStateOf<ByteArray?>(null) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val guesses = remember { mutableStateListOf<EditableGuess>() }

    fun analyze(bytes: ByteArray) {
        loading = true; message = null; guesses.clear()
        scope.launch {
            try {
                val r = c.analyzer.analyze(bytes, state.settings)
                guesses.addAll(r.items.map { EditableGuess(it) })
                message = r.message
            } catch (e: Exception) {
                message = e.message ?: "Erro a analisar a foto."
            }
            loading = false
        }
    }

    val picker = rememberImagePicker { bytes -> if (bytes != null) { image = bytes; analyze(bytes) } }
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (autoOpenCamera && !opened) { opened = true; picker.takePhoto() } }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.takePhoto() }) { Icon(Icons.Default.CameraAlt, null); Text("  Câmara") }
                OutlinedButton(onClick = { picker.pickFromGallery() }) { Icon(Icons.Default.PhotoLibrary, null); Text("  Galeria") }
            }
            Hint("Modo: ${state.settings.analyzerMode.label}")
        }
        image?.let { bytes ->
            item {
                val bmp = remember(bytes) { decodeImage(bytes) }
                if (bmp != null) Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)))
            }
        }
        if (loading) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(24.dp)); Text("  A analisar…") } }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.tertiary) } }
        items(guesses) { g ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(g.name, { g.name = it }, label = { Text("Alimento") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = { guesses.remove(g) }) { Icon(Icons.Default.Close, "Remover") }
                }
                g.confidence?.let { Hint("Certeza: ${(it * 100).roundToInt()}%" + if (g.source == EntrySource.PHOTO_LOCAL) " (modelo local)" else " (Gemini)") }
                if (g.alternatives.isNotEmpty()) {
                    Hint("Não é isto? Escolhe:")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        g.alternatives.forEach { alt ->
                            AssistChip(onClick = {
                                val previous = Pair(g.name, Triple(g.baseGrams, g.base, g.confidence ?: 0.0))
                                g.name = alt.name; g.baseGrams = alt.grams; g.base = alt.macros
                                g.alternatives = g.alternatives.map {
                                    if (it == alt) pt.guilherme.fit.ai.Alternative(previous.first, previous.second.first, previous.second.second, previous.second.third) else it
                                }
                            }, label = { Text("${alt.name} ${(alt.confidence * 100).roundToInt()}%") })
                        }
                    }
                }
                val m = g.base * g.factor.toDouble()
                Text("Porção: x${fmt(g.factor.toDouble(), 2)}" + (g.baseGrams?.let { " ≈ ${(it * g.factor).roundToInt()} g" } ?: ""))
                Slider(value = g.factor, onValueChange = { g.factor = (it * 4).roundToInt() / 4f }, valueRange = 0.25f..3f, steps = 10)
                MacroLine(m.kcal, m.protein, m.carbs, m.fat)
            }
        }
        if (guesses.isNotEmpty()) {
            item {
                val total = guesses.fold(Macros()) { a, g -> a + g.base * g.factor.toDouble() }
                Text("Total: ${total.kcal.roundToInt()} kcal", fontWeight = FontWeight.SemiBold)
                Button(onClick = {
                    guesses.forEach { g ->
                        c.addFood(
                            FoodEntry(
                                AppStore.newId(), day, slot, g.name.ifBlank { "Refeição" },
                                g.baseGrams?.let { it * g.factor }, g.base * g.factor.toDouble(), g.source,
                            )
                        )
                    }
                    onDone()
                }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar ao diário") }
            }
        }
    }
}

@Composable
private fun SearchTab(c: AppController, day: Long, slot: MealSlot, onDone: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Food?>(null) }
    var grams by remember { mutableStateOf<Double?>(100.0) }

    val sel = selected
    if (sel != null) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(sel.name, style = MaterialTheme.typography.titleMedium)
            Hint("Por 100 g: ${sel.per100.kcal.roundToInt()} kcal")
            NumberField("Quantidade", grams, { grams = it }, Modifier.fillMaxWidth(), suffix = "g", decimals = 0)
            if (sel.unitGrams != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 3).forEach { n ->
                        AssistChip(onClick = { grams = sel.unitGrams * n }, label = { Text("$n ${sel.unitName ?: "un."}") })
                    }
                }
            }
            val m = sel.per100 * ((grams ?: 0.0) / 100.0)
            MacroLine(m.kcal, m.protein, m.carbs, m.fat)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { selected = null }) { Text("Voltar") }
                Button(enabled = (grams ?: 0.0) > 0, onClick = {
                    c.addFood(FoodEntry(AppStore.newId(), day, slot, sel.name, grams, m, EntrySource.DATABASE))
                    onDone()
                }) { Text("Adicionar") }
            }
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(query, { query = it }, label = { Text("Procurar alimento") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        val results = remember(query) { FoodDb.search(query, 40) }
        LazyColumn {
            items(results) { f ->
                Column(Modifier.fillMaxWidth().clickable { selected = f; grams = f.unitGrams ?: 100.0 }.padding(vertical = 10.dp)) {
                    Text(f.name)
                    Hint("${f.per100.kcal.roundToInt()} kcal / 100 g • P ${fmt(f.per100.protein)} • H ${fmt(f.per100.carbs)} • G ${fmt(f.per100.fat)}")
                }
                HorizontalDivider()
            }
            item { Hint("Não encontras? Usa o separador Manual ou tira uma foto.") }
        }
    }
}

@Composable
private fun RecipeTab(c: AppController, day: Long, slot: MealSlot, onDone: () -> Unit) {
    val state by c.state.collectAsState()
    val profile = state.profile ?: return
    val list = remember(state.pantry, profile, slot) {
        RecipeEngine.suggest(c.recipes, profile, state.pantry, state.kitchenTools, slot, respectTime = false)
    }
    LazyColumn {
        if (list.isEmpty()) item { Hint("Nenhuma receita encaixa no teu perfil para esta refeição.") }
        items(list) { s ->
            var servings by remember { mutableStateOf(1.0) }
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(s.recipe.name)
                    val m = s.info.perServing * servings
                    MacroLine(m.kcal, m.protein, m.carbs, m.fat)
                }
                TextButton(onClick = { servings = if (servings >= 2.0) 0.5 else servings + 0.5 }) { Text("x${fmt(servings)}") }
                Button(onClick = { c.logRecipe(s.recipe, servings, day, slot); onDone() }) { Text("+") }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ManualTab(c: AppController, day: Long, slot: MealSlot, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var kcal by remember { mutableStateOf<Double?>(null) }
    var p by remember { mutableStateOf<Double?>(null) }
    var carb by remember { mutableStateOf<Double?>(null) }
    var fat by remember { mutableStateOf<Double?>(null) }
    var grams by remember { mutableStateOf<Double?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Kcal", kcal, { kcal = it }, Modifier.weight(1f), decimals = 0)
            NumberField("Gramas", grams, { grams = it }, Modifier.weight(1f), decimals = 0)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("Proteína", p, { p = it }, Modifier.weight(1f), suffix = "g")
            NumberField("Hidratos", carb, { carb = it }, Modifier.weight(1f), suffix = "g")
            NumberField("Gordura", fat, { fat = it }, Modifier.weight(1f), suffix = "g")
        }
        Hint("Se não souberes as kcal, preenche os macros e são calculadas (4/4/9).")
        Button(
            enabled = name.isNotBlank() && (kcal != null || p != null || carb != null || fat != null),
            onClick = {
                val pp = p ?: 0.0; val cc = carb ?: 0.0; val ff = fat ?: 0.0
                val k = kcal ?: (pp * 4 + cc * 4 + ff * 9)
                c.addFood(FoodEntry(AppStore.newId(), day, slot, name.trim(), grams, Macros(k, pp, cc, ff), EntrySource.MANUAL))
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Adicionar") }
    }
}
