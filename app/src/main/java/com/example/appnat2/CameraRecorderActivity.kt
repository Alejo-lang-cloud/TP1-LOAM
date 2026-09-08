package com.example.appnat2

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraRecorderActivity : AppCompatActivity() {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isFrontCamera: Boolean = true

    private lateinit var viewFinder: PreviewView
    private lateinit var btnRecordVideo: MaterialButton
    private lateinit var tvCameraTitle: TextView
    private lateinit var tvRecordingTimer: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_recorder)

        viewFinder = findViewById(R.id.view_finder)
        btnRecordVideo = findViewById(R.id.btn_record_video)
        tvCameraTitle = findViewById(R.id.tv_camera_title)
        tvRecordingTimer = findViewById(R.id.tv_recording_timer)
        val btnBackCamera = findViewById<ImageButton>(R.id.btn_back_camera)

        isFrontCamera = intent.getBooleanExtra("USE_FRONT_CAMERA", true)

        tvCameraTitle.text = if (isFrontCamera) "Grabar Video Selfie (Frontal)" else "Grabar Video Principal (Trasero)"

        btnBackCamera.setOnClickListener {
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            finish()
        }

        btnRecordVideo.setOnClickListener {
            captureVideo()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    videoCapture
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
                Toast.makeText(this, "Error al iniciar la cámara", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureVideo() {
        val currentVideoCapture = videoCapture ?: return

        btnRecordVideo.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, if (isFrontCamera) "VIDEO_SELFIE_$name" else "VIDEO_PRINCIPAL_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VAlert")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = currentVideoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(this@CameraRecorderActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        btnRecordVideo.text = "Detener Grabación"
                        btnRecordVideo.isEnabled = true
                        tvRecordingTimer.visibility = View.VISIBLE
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = if (isFrontCamera) {
                                "Video selfie grabado y guardado exitosamente en tu dispositivo"
                            } else {
                                "Video grabado y guardado exitosamente en tu dispositivo"
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        } else {
                            recording?.close()
                            recording = null
                            Toast.makeText(this, "Error al guardar video", Toast.LENGTH_SHORT).show()
                        }
                        btnRecordVideo.text = "Iniciar Grabación"
                        btnRecordVideo.isEnabled = true
                        tvRecordingTimer.visibility = View.GONE
                        finish()
                    }
                }
            }
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
