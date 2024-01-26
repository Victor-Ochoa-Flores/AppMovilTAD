package com.example.tadaplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import com.example.tadaplication.databinding.ActivitySecretBinding


class SecretActivity  : AppCompatActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivitySecretBinding
    private var action = Action.QR_SCANNER

    private val requestPermissionLauncher =
        registerForActivityResult(RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecretBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabAdd.setOnClickListener {
            showOptionsDialog()
        }
    }

    private fun showOptionsDialog() {
        // Crea un diálogo con opciones
        val options = arrayOf("Reconocimiento Facial", "Escanear QR")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecciona una opción")
            .setItems(options) { _, which ->
                // Handle click on an option
                when (which) {
                    0 -> {
                        this.action = Action.FACE_DETECTION
                        requestCameraAndStart()
                    }
                    1 -> {
                        this.action = Action.QR_SCANNER
                        requestCameraAndStart()
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                // Handle cancel
                dialog.dismiss()
            }

        // Muestra el diálogo
        builder.create().show()
    }

    private fun requestCameraAndStart() {
        if (isPermissionGranted(cameraPermission)) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun startCamera() {
        when (action) {
            Action.QR_SCANNER -> startScanner()
            Action.FACE_DETECTION -> FaceDetectionActivity.startActivity(this)
        }
    }

    private fun requestCameraPermission() {
        when {
            shouldShowRequestPermissionRationale(cameraPermission) -> {
                cameraPermissionRequest(
                    positive = { openPermissionSetting() }
                )
            }
            else -> {
                requestPermissionLauncher.launch(cameraPermission)
            }
        }
    }

    private fun startScanner() {
       ScanActivity.startScanner(this)
    }
}