package pt.guilherme.fit.domain

import pt.guilherme.fit.data.Exercise
import pt.guilherme.fit.data.Exercises
import pt.guilherme.fit.model.Experience
import pt.guilherme.fit.model.Goal
import pt.guilherme.fit.model.GymEquipment
import pt.guilherme.fit.model.Pattern
import pt.guilherme.fit.model.Pattern.*
import pt.guilherme.fit.model.PlannedExercise
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.WorkoutPlan
import pt.guilherme.fit.model.WorkoutSession
import kotlin.math.abs
import kotlin.math.roundToInt

object WorkoutGenerator {

    private data class Template(val name: String, val patterns: List<Pattern>)

    private val fullA = Template("Corpo inteiro A", listOf(SQUAT, H_PUSH, H_PULL, HINGE, V_PUSH, CORE, ISO_ARMS_BI, CALF))
    private val fullB = Template("Corpo inteiro B", listOf(HINGE, V_PULL, H_PUSH, LUNGE, ISO_SHOULDER, CORE, ISO_ARMS_TRI, CALF))
    private val upper = Template("Superior", listOf(H_PUSH, H_PULL, V_PUSH, V_PULL, ISO_SHOULDER, ISO_ARMS_BI, ISO_ARMS_TRI, CORE))
    private val lower = Template("Inferior", listOf(SQUAT, HINGE, LUNGE, ISO_LEGS, CALF, CORE, ISO_LEGS, CORE))
    private val push = Template("Empurrar (peito, ombros, tríceps)", listOf(H_PUSH, V_PUSH, H_PUSH, ISO_SHOULDER, ISO_ARMS_TRI, ISO_ARMS_TRI, CORE, ISO_SHOULDER))
    private val pull = Template("Puxar (costas, bíceps)", listOf(V_PULL, H_PULL, H_PULL, ISO_SHOULDER, ISO_ARMS_BI, CORE, ISO_ARMS_BI, V_PULL))
    private val legs = Template("Pernas", listOf(SQUAT, HINGE, LUNGE, ISO_LEGS, CALF, CORE, ISO_LEGS, HINGE))

    fun weekdaysFor(days: Int): List<Int> = when (days.coerceIn(1, 6)) {
        1 -> listOf(3)
        2 -> listOf(2, 5)
        3 -> listOf(1, 3, 5)
        4 -> listOf(1, 2, 4, 5)
        5 -> listOf(1, 2, 3, 5, 6)
        else -> listOf(1, 2, 3, 4, 5, 6)
    }

    private fun split(days: Int): Pair<String, List<Template>> = when (days.coerceIn(1, 6)) {
        1 -> "Corpo inteiro" to listOf(fullA)
        2 -> "Corpo inteiro A/B" to listOf(fullA, fullB)
        3 -> "Corpo inteiro A/B/A" to listOf(fullA, fullB, fullA.copy(name = "Corpo inteiro A (2)"))
        4 -> "Superior / Inferior" to listOf(upper, lower, upper.copy(name = "Superior (2)"), lower.copy(name = "Inferior (2)"))
        5 -> "Superior / Inferior + Empurrar / Puxar / Pernas" to listOf(upper, lower, push, pull, legs)
        else -> "Empurrar / Puxar / Pernas x2" to listOf(push, pull, legs, push.copy(name = "Empurrar (2)"), pull.copy(name = "Puxar (2)"), legs.copy(name = "Pernas (2)"))
    }

    private fun preferredLevel(exp: Experience) = when (exp) {
        Experience.BEGINNER -> 1
        Experience.INTERMEDIATE -> 2
        Experience.ADVANCED -> 3
    }

    /** Ordena as opções de um padrão: nível próximo da experiência e com carga primeiro. */
    private fun ranked(options: List<Exercise>, exp: Experience): List<Exercise> {
        val pref = preferredLevel(exp)
        return options.sortedWith(
            compareBy<Exercise> { if (it.loadable) 0 else 1 }
                .thenBy { abs(it.level - pref) }
                .thenBy { it.id }
        )
    }

    fun exerciseCount(sessionMinutes: Int, cardioMinutes: Int): Int {
        val m = sessionMinutes - cardioMinutes - 5 // 5 min de aquecimento
        return when {
            m <= 25 -> 3
            m <= 35 -> 4
            m <= 45 -> 5
            m <= 55 -> 6
            m <= 70 -> 7
            else -> 8
        }
    }

    fun cardioMinutes(goal: Goal, sessionMinutes: Int): Int = when (goal) {
        Goal.LOSE -> if (sessionMinutes >= 45) 15 else 10
        Goal.MAINTAIN -> if (sessionMinutes >= 60) 10 else 0
        Goal.GAIN -> 0
    }

    /** Padrões alternativos quando não há equipamento para o padrão pedido. */
    private val substitutes: Map<Pattern, List<Pattern>> = mapOf(
        V_PULL to listOf(H_PULL),
        H_PULL to listOf(V_PULL),
        ISO_ARMS_BI to listOf(H_PULL, V_PULL),
        ISO_ARMS_TRI to listOf(H_PUSH, V_PUSH),
        ISO_SHOULDER to listOf(V_PUSH, H_PULL),
        ISO_LEGS to listOf(LUNGE, SQUAT, HINGE),
        V_PUSH to listOf(H_PUSH),
        CALF to listOf(LUNGE),
    )

    private fun isCompound(p: Pattern) = p in setOf(SQUAT, HINGE, LUNGE, H_PUSH, V_PUSH, H_PULL, V_PULL)

    private fun prescription(ex: Exercise, goal: Goal, exp: Experience): PlannedExercise {
        val compound = isCompound(ex.pattern)
        val sets = when {
            ex.pattern == CORE || ex.pattern == CALF -> 3
            goal == Goal.GAIN && compound && exp != Experience.BEGINNER -> 4
            else -> 3
        }
        val reps = ex.repsOverride ?: when {
            !ex.loadable -> if (exp == Experience.BEGINNER) "8-12" else "10-15 (ou até 2 da falha)"
            goal == Goal.GAIN -> if (compound) "6-10" else "10-15"
            else -> if (compound) "8-12" else "12-15"
        }
        val rest = when {
            ex.pattern == CORE -> 45
            goal == Goal.GAIN && compound -> 120
            compound -> 90
            else -> 60
        }
        return PlannedExercise(ex.id, sets, reps, rest)
    }

    fun generate(profile: Profile, equipment: Set<GymEquipment>, today: Long): WorkoutPlan {
        val (splitName, templates) = split(profile.trainingDaysPerWeek)
        val weekdays = weekdaysFor(templates.size)
        val available = Exercises.available(equipment)
        val cardio = cardioMinutes(profile.goal, profile.sessionMinutes)
        val n = exerciseCount(profile.sessionMinutes, cardio)
        // conta quantas vezes cada padrão já foi usado, para variar o exercício entre sessões
        val patternUse = mutableMapOf<Pattern, Int>()

        val sessions = templates.mapIndexed { idx, t ->
            val chosen = mutableListOf<PlannedExercise>()
            val usedIds = mutableSetOf<String>()
            for (p in t.patterns + t.patterns.flatMap { substitutes[it].orEmpty() } + listOf(CORE, LUNGE, SQUAT, HINGE)) {
                if (chosen.size >= n) break
                val opts = ranked(available.filter { it.pattern == p && it.id !in usedIds }, profile.experience)
                if (opts.isEmpty()) continue
                val k = patternUse[p] ?: 0
                // as duas melhores opções vão alternando entre sessões
                val top = opts.filter { it.loadable == opts[0].loadable }
                val ex = top[k % minOf(2, top.size)]
                patternUse[p] = k + 1
                usedIds += ex.id
                chosen += prescription(ex, profile.goal, profile.experience)
            }
            WorkoutSession(t.name, weekdays[idx], chosen, cardio)
        }
        return WorkoutPlan(today, splitName, sessions)
    }

    /** Estimativa de kcal gastas numa sessão (MET ~5 para força, ~7 para cardio moderado). */
    fun estimateKcal(session: WorkoutSession, weightKg: Double, sessionMinutes: Int): Int {
        val strengthMin = (sessionMinutes - session.cardioMinutes).coerceAtLeast(0)
        val kcal = 5.0 * weightKg * strengthMin / 60.0 + 7.0 * weightKg * session.cardioMinutes / 60.0
        return kcal.roundToInt()
    }
}
