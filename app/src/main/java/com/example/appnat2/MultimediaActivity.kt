package com.example.appnat2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
            when (pendingAction) {
                ActionType.VIDEO_FRONTAL -> launchVideoRecorder()
                ActionType.VIDEO_SELFIE -> launchVideoRecorder()
                ActionType.AUDIO_MIC -> toggleAudioRecording()
                null -> {}
            }
        } else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show()
        }
        pendingAction = null
    }

    private val videoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(
                this,
                "Video grabado y guardado exitosamente en el dispositivo",
                Toast.LENGTH_LONG
            ).show()
        }
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
                launchVideoRecorder()
            }
        }

        binding.btnVideoSelfie.setOnClickListener {
            checkPermissionAndRun(Manifest.permission.CAMERA, ActionType.VIDEO_SELFIE) {
                launchVideoRecorder()
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

    private fun launchVideoRecorder() {
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra("android.intent.extras.CAMERA_FACING", 1)
            putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
        try {
            videoLauncher.launch(intent)
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

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    private fun stopAudioRecording() {
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

    override fun onStop() {
        super.onStop()
        if (isAudioRecording) {
            stopAudioRecording()
        }
    }
}
