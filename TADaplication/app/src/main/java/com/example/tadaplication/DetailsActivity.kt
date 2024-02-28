package com.example.tadaplication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tadaplication.databinding.ActivityDetailsBinding

class DetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val correo = intent.getStringExtra("correo")
        val nombre = intent.getStringExtra("nombre")

        binding.textViewCorreo.text = correo
        binding.textViewNombre.text = nombre

        binding.buttonDelete.setOnClickListener {
            // Lógica para borrar la cuenta
            val dbHelper = MyDatabaseHelper(this)
            dbHelper.deleteOneRow(intent.getStringExtra("id"))

            val resultIntent = Intent()
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}