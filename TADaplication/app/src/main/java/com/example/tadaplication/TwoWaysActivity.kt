package com.example.tadaplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import com.example.tadaplication.databinding.ActivityTwowaysBinding


class TwoWaysActivity : AppCompatActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivityTwowaysBinding
    private var action = Action.QR_SCANNER

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwowaysBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonOpenScanner.setOnClickListener {
            this.action = Action.QR_SCANNER
            requestCameraAndStart()
        }

        /*binding.buttonFaceDetect.setOnClickListener {
            this.action = Action.FACE_DETECTION
            requestCameraAndStart()
        //}*/
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
           // Action.FACE_DETECTION -> FaceDetectionActivity.startActivity(this)
            else -> {}
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
        ScannActivity.startScanner(this)
    }
}