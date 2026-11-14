package pt.guilherme.fit.domain

import pt.guilherme.fit.model.DietType
import pt.guilherme.fit.model.Goal
import pt.guilherme.fit.model.Profile
import pt.guilherme.fit.model.Sex
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class Targets(
    val bmr: Double,
    val tdee: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val waterMl: Int,
    /** negativo = défice diário, positivo = excedente */
    val dailyDelta: Double,
    val weeksToGoal: Double?,
    val notes: List<String>,
)

object Nutrition {
    const val KCAL_PER_KG = 7700.0

    /** Taxa metabólica basal (Mifflin-St Jeor). */
    fun bmr(p: Profile): Double {
        val base = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age
        return if (p.sex == Sex.MALE) base + 5 else base - 161
    }

    fun bmi(p: Profile): Double = p.weightKg / ((p.heightCm / 100) * (p.heightCm / 100))

    /**
     * @param activeKcalToday kcal ativas lidas do Apple Health / Health Connect
     * @param useHealthActive se true, usa as kcal ativas reais em vez do fator de atividade
     */
    fun targets(p: Profile, activeKcalToday: Double? = null, useHealthActive: Boolean = false): Targets {
        val notes = mutableListOf<String>()
        val bmr = bmr(p)
        val tdee = if (useHealthActive && activeKcalToday != null) {
            notes += "Gasto calculado com as kcal ativas de hoje (${activeKcalToday.roundToInt()} kcal)."
            bmr * 1.1 + activeKcalToday
        } else {
            bmr * p.activity.factor
        }

        val rate = p.weeklyRateKg.coerceIn(0.1, 1.0)
        val rawDelta = rate * KCAL_PER_KG / 7.0
        val floor = if (p.sex == Sex.MALE) 1500.0 else 1200.0

        var delta: Double
        var kcal: Double
        when (p.goal) {
            Goal.LOSE -> {
                delta = -min(rawDelta, tdee * 0.25)
                if (-delta < rawDelta) notes += "Défice limitado a 25% do gasto para não perderes massa muscular."
                kcal = tdee + delta
                if (kcal < floor) {
                    kcal = floor
                    delta = kcal - tdee
                    notes += "Calorias no mínimo seguro (${floor.roundToInt()} kcal)."
                }
                if (p.targetWeightKg >= p.weightKg) notes += "O peso-alvo não é inferior ao atual: revê o objetivo."
            }
            Goal.GAIN -> {
                delta = min(rawDelta, 500.0)
                kcal = tdee + delta
                if (p.targetWeightKg <= p.weightKg) notes += "O peso-alvo não é superior ao atual: revê o objetivo."
            }
            Goal.MAINTAIN -> {
                delta = 0.0
                kcal = tdee
            }
        }

        // Proteína: g por kg (usa o peso-alvo se o IMC for alto)
        val refWeight = if (bmi(p) > 30 && p.targetWeightKg in 40.0..p.weightKg) p.targetWeightKg else p.weightKg
        val proteinPerKg = when {
            p.dietType == DietType.HIGH_PROTEIN -> 2.2
            p.goal == Goal.LOSE -> 2.0
            p.goal == Goal.GAIN -> 1.8
            else -> 1.6
        }
        val protein = refWeight * proteinPerKg

        val fatShare = when (p.dietType) {
            DietType.KETO -> 0.70
            DietType.LOW_CARB -> 0.40
            DietType.MEDITERRANEAN -> 0.35
            else -> 0.28
        }
        var fat = kcal * fatShare / 9
        var carbs = (kcal - protein * 4 - fat * 9) / 4
        val carbCap = when (p.dietType) {
            DietType.KETO -> 30.0
            DietType.LOW_CARB -> 100.0
            else -> Double.MAX_VALUE
        }
        if (carbs > carbCap) {
            carbs = carbCap
            fat = (kcal - protein * 4 - carbs * 4) / 9
        }
        if (carbs < 0) {
            carbs = 0.0
            fat = max(0.0, (kcal - protein * 4) / 9)
        }

        val water = ((p.weightKg * 35) / 100).roundToInt() * 100

        val weeks = when (p.goal) {
            Goal.LOSE -> if (p.targetWeightKg < p.weightKg && delta < 0) (p.weightKg - p.targetWeightKg) / (-delta * 7 / KCAL_PER_KG) else null
            Goal.GAIN -> if (p.targetWeightKg > p.weightKg && delta > 0) (p.targetWeightKg - p.weightKg) / (delta * 7 / KCAL_PER_KG) else null
            Goal.MAINTAIN -> null
        }

        return Targets(bmr, tdee, kcal, protein, carbs, fat, water, delta, weeks, notes)
    }

    /** Variação de peso esperada (kg) para um saldo calórico acumulado. */
    fun expectedChangeKg(totalDeltaKcal: Double): Double = totalDeltaKcal / KCAL_PER_KG

    fun formatWeeks(weeks: Double): String {
        val w = abs(weeks)
        return if (w < 9) "${w.roundToInt()} semanas" else "${(w / 4.345).roundToInt()} meses"
    }
}
