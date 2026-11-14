package pt.guilherme.fit.platform

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.coroutines.resume

/**
 * Ligação ao Health Connect (Android). Lê passos, kcal ativas, minutos de exercício e peso
 * (o que outras apps — Google Fit, Samsung Health, Strava, relógios — lá escrevem) e escreve água, peso e nutrição.
 *
 * @param requestPermissions abre o ecrã de permissões do Health Connect (registado na MainActivity)
 */
class HealthConnectBridge(
    private val context: Context,
    private val requestPermissions: (Set<String>, (Set<String>) -> Unit) -> Unit,
) : HealthBridge {

    override val sourceName = "Health Connect"

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
    )

    private val client: HealthConnectClient? by lazy {
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
    }

    override fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override suspend fun requestPermissions(): Boolean {
        val c = client ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        if (granted.containsAll(permissions)) return true
        val result = suspendCancellableCoroutine { cont ->
            requestPermissions(permissions) { cont.resume(it) }
        }
        return result.isNotEmpty()
    }

    private suspend fun granted(): Set<String> = client?.permissionController?.getGrantedPermissions() ?: emptySet()

    override suspend fun readToday(): HealthSnapshot {
        val c = client ?: return HealthSnapshot()
        val g = granted()
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val now = Instant.now()
        val metrics = buildSet<androidx.health.connect.client.aggregate.AggregateMetric<*>> {
            if (HealthPermission.getReadPermission(StepsRecord::class) in g) add(StepsRecord.COUNT_TOTAL)
            if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in g) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in g) add(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)
        }
        var steps: Long? = null
        var active: Double? = null
        var exercise: Double? = null
        if (metrics.isNotEmpty()) {
            val agg = c.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = TimeRangeFilter.between(start, now)))
            steps = agg[StepsRecord.COUNT_TOTAL]
            active = agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories
            exercise = agg[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toDouble()
        }
        var weight: Double? = null
        if (HealthPermission.getReadPermission(WeightRecord::class) in g) {
            val res = c.readRecords(
                ReadRecordsRequest(WeightRecord::class, timeRangeFilter = TimeRangeFilter.after(now.minus(Duration.ofDays(30))))
            )
            weight = res.records.maxByOrNull { it.time }?.weight?.inKilograms
        }
        return HealthSnapshot(steps, active, exercise, weight)
    }

    private fun offset(): ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())

    override suspend fun writeWater(ml: Int) {
        val c = client ?: return
        val end = Instant.now()
        c.insertRecords(
            listOf(
                HydrationRecord(
                    startTime = end.minusSeconds(60), startZoneOffset = offset(),
                    endTime = end, endZoneOffset = offset(),
                    volume = Volume.milliliters(ml.toDouble()),
                )
            )
        )
    }

    override suspend fun writeWeight(kg: Double) {
        val c = client ?: return
        c.insertRecords(listOf(WeightRecord(time = Instant.now(), zoneOffset = offset(), weight = Mass.kilograms(kg))))
    }

    override suspend fun writeNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double) {
        val c = client ?: return
        val end = Instant.now()
        c.insertRecords(
            listOf(
                NutritionRecord(
                    startTime = end.minusSeconds(60), startZoneOffset = offset(),
                    endTime = end, endZoneOffset = offset(),
                    energy = Energy.kilocalories(kcal),
                    protein = Mass.grams(protein),
                    totalCarbohydrate = Mass.grams(carbs),
                    totalFat = Mass.grams(fat),
                )
            )
        )
    }
}
