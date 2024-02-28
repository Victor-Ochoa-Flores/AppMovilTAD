package com.example.tadaplication
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
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
import com.google.mlkit.vision.face.Face
import kotlin.math.ceil
import java.util.Base64
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets


class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private var currentStep = 0
    private val jsonObject = JSONObject()
    private val cameraXViewModel = viewModels<CameraXViewModel>()


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
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

                jsonObject.put(tipoCara, base64Image)

                if (tipoCara == "PerfilDefrente") {
                    //sendImageToServer(jsonObject)
                    // Reiniciar el objeto JSON después de enviar las fotos
                    jsonObject.remove("fotoPerfilIzquierdo")
                    jsonObject.remove("fotoPerfilDerecho")
                    jsonObject.remove("fotoPerfilDefrente")
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("Problema", "Error in takePhoto: ${e.message}")
        }
    }


    private fun sendImageToServer(jsonObject: JSONObject) {
        val apiUrl = "https://tu-servidor.com/tu-endpoint"

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val output: OutputStream = connection.outputStream
            output.write(jsonObject.toString().toByteArray(StandardCharsets.UTF_8))
            output.close()

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                Log.i("conexion", responseBody)

                val responseJson = JSONObject(responseBody)
                val token = responseJson.optString("token", "")
                val nombre = responseJson.optString("nombre", "")
                val correo = responseJson.optString("correo", "")

                // Use the extracted token as needed
                Log.i("conexion", "Token from response: $token")
                Log.i("nombre", "Token from response: $nombre")
                Log.i("correo", "Token from response: $correo")

            } else {
                Log.i("conexion", "No funcionó")
            }

            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
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
            2 -> binding.instructionText.text = "Finalizado"
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