package com.example.appnat2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appnat2.databinding.ActivityMultimediaBinding
import java.io.File

class MultimediaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMultimediaBinding
    private var mediaRecorder: MediaRecorder? = null
    private var isAudioRecording = false
    private var pendingAction: ActionType? = null

    private enum class ActionType {
        VIDEO_FRONTAL,
        VIDEO_SELFIE,
        AUDIO_MIC
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            when (pendingAction) { //una vez que tenemos permisos a la camara, vemos si es selfie o no
                ActionType.VIDEO_FRONTAL -> launchCameraRecorder(isSelfie = false)
                ActionType.VIDEO_SELFIE -> launchCameraRecorder(isSelfie = true)
                ActionType.AUDIO_MIC -> toggleAudioRecording()
                null -> {}
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
        pendingAction = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMultimediaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnVideoFrontal.setOnClickListener {
            checkPermissionAndRun(Manifest.permission.CAMERA, ActionType.VIDEO_FRONTAL) {
                launchCameraRecorder(isSelfie = false)
            }
        }

        binding.btnVideoSelfie.setOnClickListener {
            checkPermissionAndRun(Manifest.permission.CAMERA, ActionType.VIDEO_SELFIE) {
                launchCameraRecorder(isSelfie = true)
            }
        }

        binding.btnAudioMic.setOnClickListener {
            checkPermissionAndRun(Manifest.permission.RECORD_AUDIO, ActionType.AUDIO_MIC) {
                toggleAudioRecording()
            }
        }
    }

    private fun checkPermissionAndRun(permission: String, action: ActionType, block: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            block()
        } else {
            pendingAction = action
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun launchCameraRecorder(isSelfie: Boolean) {
        try { //abre el activity para camerarecorder y le pasa front_camera
            val intent = Intent(this, CameraRecorderActivity::class.java).apply {
                putExtra("USE_FRONT_CAMERA", isSelfie)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "No se pudo abrir la cámara de video", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleAudioRecording() {
        if (!isAudioRecording) {
            startAudioRecording()
        } else {
            stopAudioRecording()
        }
    }

    private fun startAudioRecording() {
        try {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: filesDir
            val audioFile = File.createTempFile("AUDIO_ALERTA_", ".m4a", storageDir)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //para grabar el audio
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            isAudioRecording = true
            binding.tvRecordingStatus.visibility = View.VISIBLE
            binding.tvAudioTitle.text = "3. Detener Grabación de Audio"
            Toast.makeText(this, "Grabando audio... Toca la opción 3 para finalizar", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al iniciar la grabación de audio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopAudioRecording() { //comenzamos la grabación de audio
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isAudioRecording = false
            binding.tvRecordingStatus.visibility = View.GONE
            binding.tvAudioTitle.text = "3. Grabar Audio con Micrófono"
            Toast.makeText(this, "Audio grabado y guardado exitosamente en el dispositivo", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el audio", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStop() { //detiene la grabación de audio
        super.onStop()
        if (isAudioRecording) {
            stopAudioRecording()
        }
    }
}
