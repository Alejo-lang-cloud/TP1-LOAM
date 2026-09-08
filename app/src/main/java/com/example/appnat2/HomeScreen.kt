package com.example.appnat2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.load
import com.example.appnat2.databinding.ScreenHomeBinding
import com.example.appnat2.ui.theme.WeatherResponse
import com.example.appnat2.ui.theme.WeatherService
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    fun turnOnFlashlight(binding: ScreenHomeBinding? = null) {
        if (!isFlashOn) {
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
                    cameraManager.setTorchMode(cameraId, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isFlashOn = true
            binding?.let {
                it.tvEstadoLinterna.text = "Linterna encendida"
                it.tvEstadoLinterna.setTextColor(Color.parseColor("#2E7D32"))
                it.ivLinternaEstado.setColorFilter(Color.parseColor("#FFB300"))
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

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textSpoken = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""

            if (textSpoken.isNotEmpty()) {
                when {
                    textSpoken.contains("prender linterna") || textSpoken.contains("encender linterna") || textSpoken.contains("activar linterna") || textSpoken.contains("prender la linterna") -> {
                        turnOnFlashlight()
                        Toast.makeText(context, "VAlert Voz: Linterna encendida", Toast.LENGTH_SHORT).show()
                    }
                    textSpoken.contains("apagar linterna") || textSpoken.contains("desactivar linterna") || textSpoken.contains("apagar la linterna") -> {
                        turnOffFlashlight()
                        Toast.makeText(context, "VAlert Voz: Linterna apagada", Toast.LENGTH_SHORT).show()
                    }
                    textSpoken.contains("mapa") || textSpoken.contains("gps") || textSpoken.contains("ubicacion") || textSpoken.contains("donde estoy") -> {
                        val intent = Intent(context, MapActivity::class.java)
                        context.startActivity(intent)
                        Toast.makeText(context, "VAlert Voz: Abriendo Mapa", Toast.LENGTH_SHORT).show()
                    }
                    textSpoken.contains("multimedia") || textSpoken.contains("camara") || textSpoken.contains("grabar") || textSpoken.contains("video") -> {
                        val intent = Intent(context, MultimediaActivity::class.java)
                        context.startActivity(intent)
                        Toast.makeText(context, "VAlert Voz: Abriendo Multimedia", Toast.LENGTH_SHORT).show()
                    }
                    textSpoken.contains("informacion") || textSpoken.contains("instructivo") || textSpoken.contains("guia") || textSpoken.contains("manual") -> {
                        val intent = Intent(context, WebViewActivity::class.java).apply {
                            putExtra("EXTRA_URL", "https://www.ready.gov/es")
                            putExtra("EXTRA_TITULO", "Instructivos de Emergencia")
                        }
                        context.startActivity(intent)
                        Toast.makeText(context, "VAlert Voz: Abriendo Instructivos de Emergencia", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Comando no reconocido: \"$textSpoken\".\nPrueba decir: 'prender linterna' o 'abrir mapa'", Toast.LENGTH_LONG).show()
                    }
                }
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
        modifier = Modifier.fillMaxSize(),
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

            fun ejecutarAlarmaCatastrofe() {
                // 1. VIBRACIÓN (9 segundos)
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(9000, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(9000)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. SONIDO IRRITANTE DE ALARMA (9 segundos)
                var ringtone: android.media.Ringtone? = null
                try {
                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = RingtoneManager.getRingtone(ctx, alarmUri)
                    ringtone?.play()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. EFECTO DE LUCES EN LA APP (Destellos durante 9 segundos)
                val mainHandler = Handler(Looper.getMainLooper())
                var flashCount = 0
                val originalBgColor = Color.parseColor("#F5F5F5")

                val flashRunnable = object : Runnable {
                    override fun run() {
                        if (flashCount < 18) {
                            val color = if (flashCount % 3 == 0) {
                                Color.parseColor("#FFD54F")
                            } else if (flashCount % 3 == 1) {
                                Color.parseColor("#E53935")
                            } else {
                                Color.parseColor("#FFFFFF")
                            }
                            binding.root.setBackgroundColor(color)
                            flashCount++
                            mainHandler.postDelayed(this, 500)
                        } else {
                            binding.root.setBackgroundColor(originalBgColor)
                            try { ringtone?.stop() } catch (_: Exception) {}
                        }
                    }
                }
                mainHandler.post(flashRunnable)

                // Actualizar estado en Firebase
                try {
                    val database = FirebaseDatabase.getInstance()
                    database.getReference("evento_Catastrofe").child("estado").setValue("ACTIVADO")
                } catch (_: Exception) {}

                // Diálogo de Emergencia
                try {
                    androidx.appcompat.app.AlertDialog.Builder(ctx)
                        .setTitle("⚠️ ALERTA DE CATÁSTROFE!")
                        .setMessage("¡BUSQUE REFUGIO Y MANTÉNGASE A SALVO!")
                        .setPositiveButton("ENTENDIDO") { dialog, _ ->
                            dialog.dismiss()
                            try { ringtone?.stop() } catch (_: Exception) {}
                            binding.root.setBackgroundColor(originalBgColor)
                        }
                        .setCancelable(false)
                        .show()
                } catch (_: Exception) {}
            }

            fun programarSimulacionCatastrofe() {
                val scheduledTime = System.currentTimeMillis() + 60_000
                val fechaTexto = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(scheduledTime))

                try {
                    val database = FirebaseDatabase.getInstance()
                    val ref = database.getReference("evento_Catastrofe")
                    val eventData = hashMapOf(
                        "fecha_hora_programada" to scheduledTime,
                        "fecha_hora_texto" to fechaTexto,
                        "estado" to "PENDIENTE",
                        "tipo" to "Simulación de Catástrofe Natural"
                    )
                    ref.setValue(eventData)
                    Toast.makeText(ctx, "⚠️ Catástrofe programada en Firebase (+1 min: $fechaTexto)", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(ctx, "Error al registrar en Firebase", Toast.LENGTH_SHORT).show()
                }

                // Programar activación exacta a 1 minuto
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    ejecutarAlarmaCatastrofe()
                }, 60_000)
            }

            binding.btnCatastrofe.setOnClickListener { programarSimulacionCatastrofe() }
            binding.cardBtnCatastrofe.setOnClickListener { programarSimulacionCatastrofe() }

            // --- INICIALIZAR VISTA PREVIA DEL MAPA ---
            binding.mapaPreview.onCreate(null)
            binding.mapaPreview.onResume()

            binding.mapaPreview.getMapAsync { googleMap ->
                val posicionInicial = com.google.android.gms.maps.model.LatLng(-36.6167, -64.2833)

                googleMap.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(posicionInicial, 13f))
                googleMap.addMarker(
                    com.google.android.gms.maps.model.MarkerOptions()
                        .position(posicionInicial)
                        .title("Ubicación Base")
                )
                googleMap.uiSettings.setAllGesturesEnabled(false)
            }

            updateUi(isFlashOn)

            binding.btnLinterna.setOnClickListener { toggleFlashlight() }

            val triggerVoiceCommand = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Di un comando (ej: 'prender linterna', 'apagar linterna', 'abrir mapa')")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(ctx, "El dispositivo no soporta reconocimiento de voz", Toast.LENGTH_SHORT).show()
                }
            }

            binding.btnVoz.setOnClickListener { triggerVoiceCommand() }
            binding.cardBtnVoz.setOnClickListener { triggerVoiceCommand() }

            val openMap = {
                val intent = Intent(ctx, MapActivity::class.java)
                ctx.startActivity(intent)
            }
            binding.btnMapaVer.setOnClickListener { openMap() }
            binding.cardMapa.setOnClickListener { openMap() }
            binding.mapaPreview.setOnClickListener { openMap() }

            val openMultimedia = {
                val intent = Intent(ctx, MultimediaActivity::class.java)
                ctx.startActivity(intent)
            }
            binding.btnCamara.setOnClickListener { openMultimedia() }
            binding.cardCamara.setOnClickListener { openMultimedia() }

            val openInformacionWeb = {
                val intent = Intent(ctx, WebViewActivity::class.java).apply {
                    putExtra("EXTRA_URL", "https://www.ready.gov/es")
                    putExtra("EXTRA_TITULO", "Instructivos de Emergencia")
                }
                ctx.startActivity(intent)
            }
            binding.btnInformacion.setOnClickListener { openInformacionWeb() }
            binding.cardInformacion.setOnClickListener { openInformacionWeb() }

            val openChatbot = {
                val intent = Intent(ctx, ChatbotActivity::class.java)
                ctx.startActivity(intent)
            }
            binding.btnChatbot.setOnClickListener { openChatbot() }
            binding.cardBtnChatbot.setOnClickListener { openChatbot() }

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
