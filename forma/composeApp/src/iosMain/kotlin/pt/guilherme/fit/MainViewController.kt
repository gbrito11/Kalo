package pt.guilherme.fit

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import pt.guilherme.fit.platform.IosClassifierAdapter
import pt.guilherme.fit.platform.IosFoodClassifier
import pt.guilherme.fit.platform.IosHealthBridge
import pt.guilherme.fit.platform.IosHealthProvider
import pt.guilherme.fit.platform.IosStorage
import pt.guilherme.fit.platform.PlatformServices
import pt.guilherme.fit.platform.createHttpClient
import pt.guilherme.fit.ui.App

/** Chamado pelo Swift: MainViewControllerKt.MainViewController(health:classifier:) */
fun MainViewController(health: IosHealthProvider, classifier: IosFoodClassifier): UIViewController =
    ComposeUIViewController {
        val services = remember {
            PlatformServices(
                storage = IosStorage(),
                health = IosHealthBridge(health),
                classifier = IosClassifierAdapter(classifier),
                http = createHttpClient(),
            )
        }
        App(services)
    }
