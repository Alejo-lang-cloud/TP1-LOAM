package com.example.appnat2

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.example.appnat2.ui.theme.Appnat2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { //compose construye la interfaz
            Appnat2Theme {
                Appnat2App()
            }
        }
    }

    override fun onStop() { //apagamos la linterna si VAlert pasa a segundo plano (ej abrir otra app)
        super.onStop()
        turnOffTorch()
    }

    override fun onDestroy() { //apagamos la linterna al salir de la app (matamos el proceso de VAlert)
        super.onDestroy()
        turnOffTorch()
    }

    private fun turnOffTorch() {
        try {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull()
            if (cameraManager != null && cameraId != null) {
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (_: Exception) {
        }
    }
}

@PreviewScreenSizes
@Composable
fun Appnat2App() { //el destino por defecto es el Home, y currentDestination me dice en cual me encuentro
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold( //genera el menu de navegación, para cada icono se le asigna currentDestination, asi desde la home accedemos a linterna, camara, etc.
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            painterResource(it.icon),
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) { //definimos el "marco" (espacio arriba y abajo) de la app con scaffold (compose)
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box( //el box ocupa todo el espacio disponible
                modifier = Modifier //modifier va indicando como se comporta cada componente del box
                    .fillMaxSize()
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding()
                    )
            ) { //cuando necesito moverme entre las tres secciones principales HOME - TELEFONO - PERFIL
                when (currentDestination) {
                    AppDestinations.HOME -> HomeScreen()
                    AppDestinations.PHONE -> TelefonoScreen()
                    AppDestinations.PROFILE -> PerfilScreen()
                }
            }
        }
    }
}

enum class AppDestinations( //enumerado con los destinos de la App, HOME, PHONE, PROFILE
    val label: String,
    val icon: Int,
) {
    HOME("Home", R.drawable.ic_home),
    PHONE("Teléfono", R.drawable.ic_phone),
    PROFILE("Perfil", R.drawable.ic_profile),
}
