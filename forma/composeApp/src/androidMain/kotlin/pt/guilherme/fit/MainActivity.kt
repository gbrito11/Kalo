package pt.guilherme.fit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import pt.guilherme.fit.platform.AndroidStorage
import pt.guilherme.fit.platform.HealthConnectBridge
import pt.guilherme.fit.platform.OnnxFoodClassifier
import pt.guilherme.fit.platform.PlatformServices
import pt.guilherme.fit.platform.createHttpClient
import pt.guilherme.fit.ui.App

class MainActivity : ComponentActivity() {

    private var onPermissions: ((Set<String>) -> Unit)? = null

    private val permissionLauncher: ActivityResultLauncher<Set<String>> =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            onPermissions?.invoke(granted)
            onPermissions = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val services = PlatformServices(
            storage = AndroidStorage(applicationContext),
            health = HealthConnectBridge(applicationContext) { perms, callback ->
                onPermissions = callback
                permissionLauncher.launch(perms)
            },
            classifier = OnnxFoodClassifier(applicationContext),
            http = createHttpClient(),
        )
        setContent { App(services) }
    }
}

/** Ecrã mostrado pelo Health Connect quando o utilizador pergunta porque pedimos permissões. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            androidx.compose.material3.MaterialTheme {
                androidx.compose.material3.Text(
                    "O Forma lê passos, calorias ativas, minutos de exercício e peso para ajustar as tuas metas, " +
                        "e escreve água, peso e nutrição que registas na app. Os dados ficam no teu telemóvel.",
                    modifier = androidx.compose.ui.Modifier.padding(24.dp),
                )
            }
        }
    }
}
