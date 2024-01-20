package com.example.tadaplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.content.DialogInterface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.example.tadaplication.databinding.ActivitySecretBinding
import com.example.tadaplication.databinding.ActivityTwowaysBinding


class SecretActivity : AppCompatActivity() {
    private lateinit var btn_add : FloatingActionButton
    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivitySecretBinding
    private var action = Action.QR_SCANNER
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySecretBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initComponentes()
        listenerAddUser()
    }

    private fun listenerAddUser(){
        btn_add.setOnClickListener(){
            showAddOptionsDialog()
        }
    }

    private fun initComponentes(){
        btn_add  = findViewById<FloatingActionButton>(R.id.addButton)
    }

    private fun showAddOptionsDialog() {

        val options = arrayOf("   Iniciar Sesión", "  Escanear QR")
        val icons = arrayOf(
            R.drawable.baseline_login_24,
            R.drawable.baseline_qr_code_24
        )
        val adapter = IconAdapter(this, options, icons)

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Como deseas agregar una nueva cuenta Cuenta")
            .setAdapter(adapter) { _, which ->
                when (which) {
                    0 -> {
                        this.action = Action.QR_SCANNER
                        requestCameraAndStart()
                    }
                    1 -> {
                        this.action = Action.QR_SCANNER
                        requestCameraAndStart()
                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
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