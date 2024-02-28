package com.example.tadaplication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.tadaplication.databinding.ActivitySecretBinding
import com.example.tadaplication.databinding.ActivityValidQractivityBinding

class validQRActivity : AppCompatActivity() {
    private lateinit var binding: ActivityValidQractivityBinding
    lateinit var token : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityValidQractivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        if (intent.hasExtra("token")) {
            token = intent.getStringExtra("token").toString()
        }

        binding.botonContinuar.setOnClickListener {
            val intent = Intent(this, FaceDetectionActivity::class.java)
            intent.putExtra("token", token)
            startActivity(intent)
        }
    }
}