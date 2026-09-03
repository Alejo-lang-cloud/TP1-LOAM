package com.example.appnat2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.load
import com.example.appnat2.databinding.ScreenHomeBinding
import com.example.appnat2.ui.theme.WeatherResponse
import com.example.appnat2.ui.theme.WeatherService

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isFlashOn by rememberSaveable { mutableStateOf(false) }

    var weatherData by remember { mutableStateOf<WeatherResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val service = WeatherService.create()
            val response = service.getCurrentWeather("Santa Rosa, LP, AR", "cb3bf36ed870f1ba1b006446093e04e9")
            weatherData = response
            errorMessage = null
        } catch (e: Exception) {
            e.printStackTrace()
            errorMessage = when {
                e.message?.contains("401") == true -> "Error: Clave API no válida o aún no activada"
                else -> "Error de conexión: No se pudo cargar el clima"
            }
        }
    }

    fun turnOffFlashlight(binding: ScreenHomeBinding? = null) {
        if (isFlashOn) {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                    try {
                        cameraManager.getCameraCharacteristics(id)
                            .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } catch (_: Exception) {
                        false
                    }
                } ?: cameraManager?.cameraIdList?.firstOrNull()

                if (cameraManager != null && cameraId != null) {
                    cameraManager.setTorchMode(cameraId, false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isFlashOn = false
            binding?.let {
                it.tvEstadoLinterna.text = "Linterna apagada"
                it.tvEstadoLinterna.setTextColor(Color.parseColor("#D32F2F"))
                it.ivLinternaEstado.setColorFilter(Color.parseColor("#E65100"))
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_PAUSE) {
                turnOffFlashlight()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            turnOffFlashlight()
        }
    }

    AndroidView(
        factory = { ctx ->
            val binding = ScreenHomeBinding.inflate(android.view.LayoutInflater.from(ctx))

            fun updateUi(enabled: Boolean) {
                if (enabled) {
                    binding.tvEstadoLinterna.text = "Linterna encendida"
                    binding.tvEstadoLinterna.setTextColor(Color.parseColor("#2E7D32"))
                    binding.ivLinternaEstado.setColorFilter(Color.parseColor("#FFB300"))
                } else {
                    binding.tvEstadoLinterna.text = "Linterna apagada"
                    binding.tvEstadoLinterna.setTextColor(Color.parseColor("#D32F2F"))
                    binding.ivLinternaEstado.setColorFilter(Color.parseColor("#E65100"))
                }
            }

            fun updateBattery(intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val pct = if (level >= 0 && scale > 0) {
                    (level * 100 / scale.toFloat()).toInt()
                } else {
                    100
                }

                binding.tvBateriaNivel.text = "$pct%"

                if (isCharging) {
                    binding.tvBateriaEstimacion.text = "(Cargando)"
                    binding.tvBateriaEstimacion.setTextColor(Color.parseColor("#2E7D32"))
                } else {
                    val remainingMinutes = pct * 5
                    val hours = remainingMinutes / 60
                    val mins = remainingMinutes % 60

                    val timeText = when {
                        hours > 0 && mins > 0 -> "(${hours}hr ${mins}min)"
                        hours > 0 -> "(${hours}hr)"
                        else -> "(${mins}min)"
                    }
                    binding.tvBateriaEstimacion.text = timeText
                    binding.tvBateriaEstimacion.setTextColor(Color.parseColor("#666666"))
                }
            }

            val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val stickyBatteryIntent = ctx.registerReceiver(null, batteryFilter)
            updateBattery(stickyBatteryIntent)

            fun toggleFlashlight() {
                val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                val cameraId = try {
                    cameraManager?.cameraIdList?.firstOrNull { id ->
                        try {
                            cameraManager.getCameraCharacteristics(id)
                                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        } catch (_: Exception) {
                            false
                        }
                    } ?: cameraManager?.cameraIdList?.firstOrNull()
                } catch (_: Exception) {
                    null
                }

                val newState = !isFlashOn

                if (cameraManager != null && cameraId != null) {
                    try {
                        cameraManager.setTorchMode(cameraId, newState)
                        isFlashOn = newState
                        updateUi(newState)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        isFlashOn = newState
                        updateUi(newState)
                        Toast.makeText(ctx, "No se pudo cambiar la linterna física", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isFlashOn = newState
                    updateUi(newState)
                    Toast.makeText(ctx, "Dispositivo sin linterna física detectada", Toast.LENGTH_SHORT).show()
                }
            }

            updateUi(isFlashOn)

            binding.btnLinterna.setOnClickListener { toggleFlashlight() }

            val openMap = {
                val intent = Intent(ctx, MapActivity::class.java)
                ctx.startActivity(intent)
            }
            binding.btnMapaVer.setOnClickListener { openMap() }
            binding.cardMapa.setOnClickListener { openMap() }
            binding.flMapaPreview.setOnClickListener { openMap() }

            val openMultimedia = {
                val intent = Intent(ctx, MultimediaActivity::class.java)
                ctx.startActivity(intent)
            }
            binding.btnCamara.setOnClickListener { openMultimedia() }
            binding.cardCamara.setOnClickListener { openMultimedia() }

            binding.btnSalir.setOnClickListener {
                turnOffFlashlight(binding)
                (ctx as? Activity)?.finish()
            }

            binding.root
        },
        update = { view ->
            val binding = ScreenHomeBinding.bind(view)

            if (errorMessage != null) {
                binding.tvClimaTemperatura.text = "--"
                binding.tvClimaDescripcion.text = errorMessage
                binding.tvClimaUbicacion.text = "Error"
            } else if (weatherData != null) {
                weatherData?.let { data ->
                    binding.tvClimaTemperatura.text = "${data.main.temp.toInt()}°C"
                    binding.tvClimaDescripcion.text = data.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: ""
                    binding.tvClimaUbicacion.text = data.name

                    val iconCode = data.weather.firstOrNull()?.icon
                    if (iconCode != null) {
                        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                        binding.ivClimaIcono.load(iconUrl)
                    }
                }
            } else {
                binding.tvClimaTemperatura.text = "..."
                binding.tvClimaDescripcion.text = "Cargando clima..."
            }

            if (isFlashOn) {
                binding.tvEstadoLinterna.text = "Linterna encendida"
                binding.tvEstadoLinterna.setTextColor(Color.parseColor("#2E7D32"))
                binding.ivLinternaEstado.setColorFilter(Color.parseColor("#FFB300"))
            } else {
                binding.tvEstadoLinterna.text = "Linterna apagada"
                binding.tvEstadoLinterna.setTextColor(Color.parseColor("#D32F2F"))
                binding.ivLinternaEstado.setColorFilter(Color.parseColor("#E65100"))
            }
        }
    )
}
