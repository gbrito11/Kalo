import Foundation
import HealthKit
import ComposeApp

/// Ligação ao Apple Health. Lê passos, kcal ativas, minutos de exercício e peso
/// (também o que o Apple Watch, Strava, Garmin, etc. lá escrevem) e escreve água, peso e nutrição.
final class HealthKitProvider: NSObject, IosHealthProvider {
    private let store = HKHealthStore()

    func isAvailable() -> Bool { HKHealthStore.isHealthDataAvailable() }

    func requestAuthorization(completion: @escaping (KotlinBoolean) -> Void) {
        guard isAvailable() else { completion(KotlinBoolean(value: false)); return }
        let read: Set<HKObjectType> = [
            HKQuantityType(.stepCount),
            HKQuantityType(.activeEnergyBurned),
            HKQuantityType(.appleExerciseTime),
            HKQuantityType(.bodyMass),
            HKObjectType.workoutType(),
        ]
        let write: Set<HKSampleType> = [
            HKQuantityType(.dietaryWater),
            HKQuantityType(.bodyMass),
            HKQuantityType(.dietaryEnergyConsumed),
            HKQuantityType(.dietaryProtein),
            HKQuantityType(.dietaryCarbohydrates),
            HKQuantityType(.dietaryFatTotal),
        ]
        store.requestAuthorization(toShare: write, read: read) { ok, _ in
            completion(KotlinBoolean(value: ok))
        }
    }

    private func sumToday(_ id: HKQuantityTypeIdentifier, unit: HKUnit, completion: @escaping (KotlinDouble) -> Void) {
        let type = HKQuantityType(id)
        let start = Calendar.current.startOfDay(for: Date())
        let predicate = HKQuery.predicateForSamples(withStart: start, end: Date(), options: .strictStartDate)
        let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { _, stats, _ in
            let value = stats?.sumQuantity()?.doubleValue(for: unit) ?? -1
            completion(KotlinDouble(value: value))
        }
        store.execute(query)
    }

    func todaySteps(completion: @escaping (KotlinDouble) -> Void) {
        sumToday(.stepCount, unit: .count(), completion: completion)
    }

    func todayActiveEnergy(completion: @escaping (KotlinDouble) -> Void) {
        sumToday(.activeEnergyBurned, unit: .kilocalorie(), completion: completion)
    }

    func todayExerciseMinutes(completion: @escaping (KotlinDouble) -> Void) {
        sumToday(.appleExerciseTime, unit: .minute(), completion: completion)
    }

    func latestWeight(completion: @escaping (KotlinDouble) -> Void) {
        let sort = NSSortDescriptor(key: HKSampleSortIdentifierEndDate, ascending: false)
        let query = HKSampleQuery(sampleType: HKQuantityType(.bodyMass), predicate: nil, limit: 1, sortDescriptors: [sort]) { _, samples, _ in
            let kg = (samples?.first as? HKQuantitySample)?.quantity.doubleValue(for: .gramUnit(with: .kilo)) ?? -1
            completion(KotlinDouble(value: kg))
        }
        store.execute(query)
    }

    private func save(_ id: HKQuantityTypeIdentifier, _ quantity: HKQuantity) {
        let now = Date()
        let sample = HKQuantitySample(type: HKQuantityType(id), quantity: quantity, start: now, end: now)
        store.save(sample) { _, _ in }
    }

    func saveWater(ml: Double) {
        save(.dietaryWater, HKQuantity(unit: .literUnit(with: .milli), doubleValue: ml))
    }

    func saveWeight(kg: Double) {
        save(.bodyMass, HKQuantity(unit: .gramUnit(with: .kilo), doubleValue: kg))
    }

    func saveNutrition(kcal: Double, protein: Double, carbs: Double, fat: Double) {
        save(.dietaryEnergyConsumed, HKQuantity(unit: .kilocalorie(), doubleValue: kcal))
        if protein > 0 { save(.dietaryProtein, HKQuantity(unit: .gram(), doubleValue: protein)) }
        if carbs > 0 { save(.dietaryCarbohydrates, HKQuantity(unit: .gram(), doubleValue: carbs)) }
        if fat > 0 { save(.dietaryFatTotal, HKQuantity(unit: .gram(), doubleValue: fat)) }
    }
}
