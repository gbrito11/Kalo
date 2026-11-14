import SwiftUI
import ComposeApp

@main
struct FormaApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(
            health: HealthKitProvider(),
            classifier: CoreMLFoodClassifier()
        )
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
