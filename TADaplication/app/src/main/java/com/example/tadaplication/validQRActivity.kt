package com.example.tadaplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tadaplication.databinding.ActivitySecretBinding
import com.example.tadaplication.databinding.ActivityValidQractivityBinding

class validQRActivity : AppCompatActivity() {
    private lateinit var binding: ActivityValidQractivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityValidQractivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botonContinuar.setOnClickListener {
            startActivity(Intent(this,FaceDetectionActivity::class.java))
        }
    }
}