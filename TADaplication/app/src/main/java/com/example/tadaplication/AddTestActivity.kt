package com.example.tadaplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.widget.Button
import android.widget.EditText

class AddTestActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextLastName: EditText
    private lateinit var buttonAddPerson: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_test)

        editTextName = findViewById(R.id.editTextName)
        editTextLastName = findViewById(R.id.editTextLastName)
        buttonAddPerson = findViewById(R.id.buttonAddPerson)

        buttonAddPerson.setOnClickListener {
            val name = editTextName.text.toString()
            val lastName = editTextLastName.text.toString()

            /*if (name.isNotEmpty() && lastName.isNotEmpty()) {
                // Save data to the database
                val dbHelper = MyDatabaseHelper(this)
                dbHelper.addCuenta(name, lastName, 1)

                // Set result and finish activity
                val resultIntent = Intent()
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }*/
        }
    }
}