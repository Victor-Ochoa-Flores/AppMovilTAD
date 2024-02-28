package com.example.tadaplication
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.AsyncTask
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.tadaplication.databinding.ActivityFaceDetectionBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executors
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import com.example.tadaplication.databinding.ActivityFaceDetectionValidarBinding
import com.google.mlkit.vision.face.Face
import org.json.JSONArray
import kotlin.math.ceil
import java.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.io.File
import java.io.FileWriter

class FaceDetectionValidarActivity : AppCompatActivity() {



    private lateinit var binding: ActivityFaceDetectionValidarBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private var currentStep = 0
    private val jsonObject = JSONObject()
    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private val handler = Handler()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding =ActivityFaceDetectionValidarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        if (intent.hasExtra("token")) {
            val token = intent.getStringExtra("token")
            jsonObject.put("token",token)
            Log.i("coordenadas", "Token from response: $token")
            // Ahora, puedes usar 'datoRecibido' en 'ActivityB'
        }

        binding.previewView.post {
            cameraSelector =
                CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()
            cameraXViewModel.value.processCameraProvider.observe(this) { provider ->
                processCameraProvider = provider
                bindCameraPreview()
                bindInputAnalyzer()
            }
        }

    }

    private fun saveJsonObjectToFile(jsonObject: JSONObject, fileName: String) {
        val jsonString = jsonObject.toString()

        try {
            val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDirectory, fileName)
            val fileWriter = FileWriter(file)
            fileWriter.write(jsonString)
            fileWriter.flush()
            fileWriter.close()
            Log.i("conexion", "JSONObject saved to file: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("conexion", "Error saving JSONObject to file: ${e.message}")
        }
    }

    private fun bindCameraPreview() {
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        cameraPreview.setSurfaceProvider(binding.previewView.surfaceProvider)

        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera preview", e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun bindInputAnalyzer() {
        val detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                .build()
        )

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(detector, imageProxy)
        }

        try {
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        } catch (e: Exception) {
            Log.e(TAG, "Error binding image analyzer", e)
        }
    }
    private fun goToSecretActivity() {
        handler.postDelayed({
            val intent = Intent(this, SecretActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            finish() // Cierra la actividad actual
            startActivity(intent)
        }, 4000)
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalGetImage::class) private fun processImageProxy(detector: FaceDetector, imageProxy: ImageProxy) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage).addOnSuccessListener { faces ->
            binding.graphicOverlay.clear()
            faces.forEach { face ->
                val faceBox = FaceBox(binding.graphicOverlay, face, imageProxy.image!!.cropRect)
                binding.graphicOverlay.add(faceBox)

                // Validar posición del rostro antes de tomar la foto

                if (isFaceInCorrectPosition(face, currentStep)) {
                    Log.i ("prueba","TOMA FOTO")
                    Thread.sleep(300)
                    takePhoto(binding.graphicOverlay,face,imageProxy.image!!.cropRect,currentStep)
                    updateInstructionText()
                    Log.i ("prueba","DESPUES DE FOTO")
                }
                else{
                    //Log.i ("prueba","entra a else no coincide con conrrenStep")
                }
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateTempToken(seedToken: String): String {
        val timestamp = System.currentTimeMillis() / 60000  // Obtiene la cantidad de minutos desde epoch
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(seedToken.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(timestamp.toString().toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SuspiciousIndentation")
    private fun takePhoto(overlay: FaceBoxOverlay, face: Face, imageRect: Rect, current: Int) {

        try {

            val bitmap = binding.previewView.bitmap ?: return
            val scaleX = overlay.width.toFloat() / imageRect.height().toFloat()
            val scaleY = overlay.height.toFloat() / imageRect.width().toFloat()
            val scale = scaleX.coerceAtLeast(scaleY)
            var izquierda : Float
            var derecha : Float
            var arriba: Float
            var abajo : Float
            var tipoCara : String

            tipoCara = ""
            overlay.mScale = scale

            val offsetX = (overlay.width.toFloat() - ceil(imageRect.height().toFloat() * scale)) / 2.0f
            val offsetY = (overlay.height.toFloat() - ceil(imageRect.width().toFloat() * scale)) / 2.0f

            overlay.mOffsetX = offsetX
            overlay.mOffsetY = offsetY


            izquierda = face.boundingBox.right * scale + offsetX
            arriba = face.boundingBox.top * scale + offsetY
            derecha = face.boundingBox.left * scale + offsetX
            abajo = face.boundingBox.bottom * scale + offsetY

            val centerX = overlay.width.toFloat() / 2

            izquierda = centerX + (centerX - izquierda)
            derecha = centerX - (derecha - centerX)

            val faceBitmap = Bitmap.createBitmap(
                bitmap,
                izquierda.toInt(),   // Ajustar la coordenada izquierda
                arriba.toInt(),      // Ajustar la coordenada arriba
                (derecha - izquierda).toInt(),  // Ancho ajustado
                (abajo - arriba).toInt()        // Altura ajustada
            )
            saveImageToGallery(faceBitmap)

            val base64Image = convertBitmapToBase64(faceBitmap)

            //Para añadir la imagen
            if (current == 0 ){
                tipoCara = "fotoPerfilDerecho"
            }
            else if (current == 1){
                tipoCara = "fotoPerfilIzquierdo"
            }
            else if (current == 2){
                tipoCara = "fotoPerfilDefrente"
            }

            if (tipoCara != "" ){

                // Update the JSON object with the array
                jsonObject.put(tipoCara, JSONArray().apply {
                    put(base64Image.toString())
                    put(base64Image.toString())
                    put(base64Image.toString())
                })


                if (tipoCara == "fotoPerfilDefrente") {
                    Log.i("conexion", "antes del SEND")

                    //saveJsonObjectToFile(jsonObject, "hola.txt")

                    val sendImageTask = SendImageToServerTask(jsonObject,this)
                    sendImageTask.execute()

                    val jsonString = jsonObject.toString()
                    Log.i("JSONObject", jsonString)

                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("Problema", "Error in takePhoto: ${e.message}")
        }
    }

    class SendImageToServerTask(private val jsonObject: JSONObject,private val contexto: Context) : AsyncTask<Void, Void, String>() {

        override fun doInBackground(vararg params: Void?): String {
            val apiUrl = "https://camend-apis.icymoss-4d00a757.eastus.azurecontainerapps.io/registrar_rostro"
            Log.i("conexion", "0")


            try {
                Log.i("conexion", "URL")

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                Log.i("conexion", "1")

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                Log.i("conexion", "2")

                val output: OutputStream = connection.outputStream
                output.write(jsonObject.toString().toByteArray(StandardCharsets.UTF_8))
                output.close()

                Log.i("conexion", "3")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }

                    Log.i("conexion", responseBody)

                    val responseJson = JSONObject(responseBody)
                    val token = responseJson.optString("token_semilla", "")
                    val nombre = responseJson.optString("Nombres", "")
                    val correo = responseJson.optString("Correo", "")

                    // Utiliza el token según sea necesario
                    Log.i("conexion", "Token from response: $token")
                    Log.i("conexion", "Nombre from response: $nombre")
                    Log.i("conexion", "Correo from response: $correo")


                    if (token.isNotEmpty()) {
                        saveSeedTokenToCache(contexto, token)
                    }

                    val storedSeedToken = getStoredSeedToken(contexto)
                    if (storedSeedToken != null) {
                        Log.i("conexion", "Token guardado: $storedSeedToken")
                    }

                    if ( token != "" && nombre != "" && correo != ""){
                        add_BDatos (contexto,nombre,correo)
                    }

                    return responseBody
                } else {
                    Log.i("conexion", "Error en la operación de red. Código de respuesta: $responseCode")

                    // Imprimir el mensaje de error si está disponible
                    val errorMessage = try {
                        connection.inputStream.bufferedReader().use { it.readText() }
                    } catch (e: Exception) {

                        Log.i("conexion", e.toString())
                    }

                    if (errorMessage != null) {
                        Log.i("conexion", "Mensaje de error: $errorMessage")
                    }


                    return "Error en la operación de red"
                }
            } catch (e: Exception) {
                Log.i("conexion", e.toString())
                e.printStackTrace()
                return "Error en la operación de red"
            }
        }
        private fun saveSeedTokenToCache(context: Context, seedToken: String) {
            val sharedPreferences =
                context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("seedToken", seedToken)
            editor.apply()
        }

        fun getStoredSeedToken(context: Context): String? {
            val sharedPreferences = context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            return sharedPreferences.getString("seedToken", null)
        }

        override fun onPostExecute(result: String) {
            // Maneja el resultado aquí (actualización de la interfaz de usuario, etc.)
            // Este método se ejecuta en el hilo principal
        }

        private fun add_BDatos(context: Context, name: String, lastName: String) {
            val dbHelper = MyDatabaseHelper(context)
            dbHelper.addCuenta(name, lastName)
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // Use java.util.Base64 for encoding
        val base64String = Base64.getEncoder().encodeToString(byteArray)

        return base64String
    }

    private fun isFaceInCorrectPosition(face: com.google.mlkit.vision.face.Face, step: Int): Boolean {
        Log.i ("prueba","entra A validar foto $step" )
        when (step) {
            0 -> {
                // Validar perfil derecho
                val headEulerAngleY = face.headEulerAngleY
                Log.i ("prueba","entra a 0 -> $headEulerAngleY" )
                return headEulerAngleY > 43 && headEulerAngleY < 55

            }
            1 -> {
                // Validar perfil izquierdo
                val headEulerAngleY = face.headEulerAngleY
                Log.i ("prueba","entra a 1 -> $headEulerAngleY")
                return headEulerAngleY < -43 && headEulerAngleY > -55

            }
            2 -> {
                // Validar frente
                val headEulerAngleY = face.headEulerAngleY
                Log.i ("prueba","entra a 2 -> $headEulerAngleY")
                return headEulerAngleY > -3 && headEulerAngleY < 3
            }
            // Agrega más casos según sea necesario
            else -> return false
        }
    }

    private fun updateInstructionText() {

        when (currentStep) {
            0 -> binding.instructionText.text = "Coloca tu perfil izquierda"
            1 -> binding.instructionText.text = "Coloca tu perfil al frente"
            2 -> {
                binding.instructionText.text = "Finalizado"
                goToSecretActivity()
            }
            // Puedes agregar más pasos según sea necesario
            else -> {
                Log.i ("prueba","entra a else de update")
            }
        }
        currentStep++
    }

    private fun saveImageToGallery(bitmap: Bitmap) {

        // Guardar la imagen en la galería utilizando la API de MediaStore
        val imageUrl = MediaStore.Images.Media.insertImage(
            contentResolver,
            bitmap,
            "FaceImage",
            "Detected face image"
        )

        if (imageUrl != null) {
            // Éxito al guardar la imagen
            Log.d(TAG, "Imagen guardada en la galería: $imageUrl")

        } else {
            Log.e(TAG, "Error al guardar la imagen en la galería")
        }
    }


    companion object {
        private val TAG = FaceDetectionActivity::class.simpleName

        fun startActivity(context: Context) {
            Intent(context, FaceDetectionActivity::class.java).also {
                context.startActivity(it)
            }
        }
    }


}