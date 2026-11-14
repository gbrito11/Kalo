package pt.guilherme.fit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import pt.guilherme.fit.AppController
import pt.guilherme.fit.model.MealSlot
import pt.guilherme.fit.platform.PlatformBackHandler
import pt.guilherme.fit.platform.PlatformServices

sealed interface Route {
    data object Profile : Route
    data class AddFood(val day: Long, val slot: MealSlot, val startWithPhoto: Boolean = false) : Route
    data class RecipeDetail(val id: String, val day: Long? = null, val slot: MealSlot? = null, val servings: Double? = null) : Route
    data class Session(val day: Long) : Route
    data class Day(val day: Long) : Route
}

enum class MainTab(val label: String, val icon: ImageVector) {
    TODAY("Hoje", Icons.Default.Home),
    FOOD("Comida", Icons.Default.Restaurant),
    TRAINING("Treino", Icons.Default.FitnessCenter),
    KITCHEN("Cozinha", Icons.Default.Kitchen),
    CALENDAR("Calendário", Icons.Default.CalendarMonth),
}

class Nav {
    val stack = mutableStateListOf<Route>()
    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
}

@Composable
fun App(services: PlatformServices) {
    val scope = rememberCoroutineScope()
    val controller = remember { AppController(services, scope) }
    FormaTheme { Root(controller) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Root(c: AppController) {
    val state by c.state.collectAsState()
    val nav = remember { Nav() }
    var tab by remember { mutableStateOf(MainTab.TODAY) }

    LaunchedEffect(Unit) { c.refreshHealth() }

    if (state.profile == null) {
        ProfileScreen(c, onboarding = true, onDone = {})
        return
    }

    val top = nav.stack.lastOrNull()
    PlatformBackHandler(enabled = top != null) { nav.pop() }

    if (top != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(titleFor(top, c)) },
                    navigationIcon = {
                        IconButton(onClick = { nav.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
                    },
                )
            },
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (top) {
                    Route.Profile -> ProfileScreen(c, onboarding = false, onDone = { nav.pop() })
                    is Route.AddFood -> AddFoodScreen(c, top.day, top.slot, top.startWithPhoto, onDone = { nav.pop() })
                    is Route.RecipeDetail -> RecipeDetailScreen(c, top, onDone = { nav.pop() })
                    is Route.Session -> SessionScreen(c, top.day)
                    is Route.Day -> DayScreen(c, top.day, nav)
                }
            }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tab.label) },
                actions = {
                    IconButton(onClick = { nav.push(Route.Profile) }) { Icon(Icons.Default.Person, "Perfil") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = t == tab,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                MainTab.TODAY -> TodayScreen(c, nav)
                MainTab.FOOD -> DiaryScreen(c, nav)
                MainTab.TRAINING -> TrainingScreen(c, nav)
                MainTab.KITCHEN -> KitchenScreen(c, nav)
                MainTab.CALENDAR -> CalendarScreen(c, nav)
            }
        }
    }
}

private fun titleFor(r: Route, c: AppController): String = when (r) {
    Route.Profile -> "Perfil e definições"
    is Route.AddFood -> "Adicionar • ${r.slot.label}"
    is Route.RecipeDetail -> c.recipe(r.id)?.name ?: "Receita"
    is Route.Session -> "Treino"
    is Route.Day -> pt.guilherme.fit.domain.Dates.long(r.day)
}
