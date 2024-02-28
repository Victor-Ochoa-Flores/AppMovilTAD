package com.example.tadaplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toolbar
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tadaplication.databinding.ActivitySecretBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SecretActivity  : AppCompatActivity() {

    private val cameraPermission = android.Manifest.permission.CAMERA
    private lateinit var binding: ActivitySecretBinding
    private var action = Action.QR_SCANNER
    private val cuentas = mutableListOf<Cuenta>()
    private lateinit var recyclerView: RecyclerView

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
        val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
        toolbar.title = "AuthApp"
        setSupportActionBar(binding.toolbar)
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = CuentaAdapter(cuentas)

        adapter.setOnItemClickListener(object : CuentaAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val selectedCuenta = cuentas[position]

                // Crear un Intent para abrir DetailsActivity y pasar los datos seleccionados
                val intent = Intent(this@SecretActivity, DetailsActivity::class.java).apply {
                    putExtra("correo", selectedCuenta.correo)
                    putExtra("nombre", selectedCuenta.nombre)
                    putExtra("id", selectedCuenta.id)
                    // Puedes pasar más datos según tus necesidades
                }

                // Iniciar DetailsActivity
                startActivityForResult(intent, DELETE_PERSON_REQUEST)
            }
        })
        recyclerView.adapter = adapter

        updateRecyclerView()
        binding.fabAdd.setOnClickListener {
            showOptionsDialog()
        }
    }

    private fun showOptionsDialog() {
        // Crea un diálogo con opciones
        val options = arrayOf("Reconocimiento Facial", "Escanear QR","Añadir usuario")
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
                    2 -> {
                        val intent = Intent(this, AddTestActivity::class.java)
                        startActivityForResult(intent, ADD_PERSON_REQUEST)

                    }
                }
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == ADD_PERSON_REQUEST && resultCode == Activity.RESULT_OK) ||(requestCode == DELETE_PERSON_REQUEST && resultCode == Activity.RESULT_OK) ) {
            // Update the RecyclerView after adding a new person
            updateRecyclerView()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRecyclerView() {

        val dbHelper = MyDatabaseHelper(this)
        val cursor = dbHelper.readAllData()

        cuentas.clear()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_CORREO))
                val lastName = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_NOMBRE))
                val id = cursor.getString(cursor.getColumnIndexOrThrow(MyDatabaseHelper.COLUMN_ID))
                cuentas.add(Cuenta(name, lastName,id))
            } while (cursor.moveToNext())
        }

        recyclerView.adapter?.notifyDataSetChanged()

        cursor?.close()
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

    companion object {
        const val ADD_PERSON_REQUEST = 1
        const val DELETE_PERSON_REQUEST = 2
    }
}