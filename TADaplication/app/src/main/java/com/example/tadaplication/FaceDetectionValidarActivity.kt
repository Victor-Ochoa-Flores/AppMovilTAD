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
    private var currentStep = 2
    private val jsonObject = JSONObject()
    private val cameraXViewModel = viewModels<CameraXViewModel>()
    private val handler = Handler()

    private lateinit var nombreAux: String
    private lateinit var CorreoAux: String
    private lateinit var idAux: String

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding =ActivityFaceDetectionValidarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val correo = intent.getStringExtra("correo")
        val nombre = intent.getStringExtra("nombre")
        val id = intent.getStringExtra("id")

        if (nombre != null && id != null && correo != null) {
            nombreAux = nombre
            CorreoAux = correo
            idAux = id
        }
        jsonObject.put("idusuario",idAux.toInt())

        /*val intent = intent
        if (intent.hasExtra("token")) {
            val token = intent.getStringExtra("token")
            jsonObject.put("token",token)
            Log.i("coordenadas", "Token from response: $token")
        }*/

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
            val intent = Intent(this, DetailsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("correo",CorreoAux )
            intent.putExtra("nombre", nombreAux )
            intent.putExtra("id", idAux)
            finish() // Cierra la actividad actual
            startActivity(intent)
        }, 5000)
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
                    Thread.sleep(300)
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


            val arrayIzquierdo = intArrayOf(1, 2, 2)
            val arrayDerecho = intArrayOf(1, 2, 2)

            jsonObject.put("fotoPerfilIzquierdo", JSONArray(arrayIzquierdo))
            jsonObject.put("fotoPerfilDerecho", JSONArray(arrayDerecho))

            // Update the JSON object with the array
                jsonObject.put("fotoPerfilDefrente", JSONArray().apply {
                    put(1)
                    put(base64Image.toString())
                    put(2)
                })

                Log.i("conexion", "antes del SEND")

                val storedSeedToken = getStoredSeedToken(this)
                jsonObject.put("token",storedSeedToken)
                //saveJsonObjectToFile(jsonObject, "hola.txt")

                val sendImageTask = SendImageToServerTask(jsonObject,this,this)
                sendImageTask.execute()



        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("Problema", "Error in takePhoto: ${e.message}")
        }
    }

    class SendImageToServerTask(private val jsonObject: JSONObject,private val contexto: Context ,  private val activity: FaceDetectionValidarActivity) : AsyncTask<Void, Void, String>() {

        @RequiresApi(Build.VERSION_CODES.O)
        override fun doInBackground(vararg params: Void?): String {
            val apiUrl = "https://camend-apis.icymoss-4d00a757.eastus.azurecontainerapps.io/validar_rostro"
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
                Thread.sleep(600)

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }

                    Log.i("conexion", responseBody)

                    val responseJson = JSONObject(responseBody)
                    val estado = responseJson.optString("Estado", "")
                    val distancia = responseJson.optString("Distancia", "")

                    Log.i("conexion", "Estado : $estado")
                    Log.i("conexion", "Distancia : $distancia")
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



        override fun onPostExecute(result: String) {

            // Maneja el resultado aquí (actualización de la interfaz de usuario, etc.)
            // Este método se ejecuta en el hilo principal
            Log.i("conexion", "se logro onpost $result")
            if(result == "Error en la operación de red"){
                activity.finish()
            }
            else{
                Log.i("conexion", "entra al else $result")
                activity.goToSecretActivity()
            }
        }

    }
    fun getStoredSeedToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
        return sharedPreferences.getString("seedToken", null)
    }

    /*@RequiresApi(Build.VERSION_CODES.O)
    fun generateTempToken(seedToken: String): String {
        val timestamp = (System.currentTimeMillis() / 60000).toString()
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(seedToken.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(timestamp.toByteArray(StandardCharsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }*/

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
                binding.instructionText.text = "Coloca tu perfil al frente"
                Thread.sleep(300)
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