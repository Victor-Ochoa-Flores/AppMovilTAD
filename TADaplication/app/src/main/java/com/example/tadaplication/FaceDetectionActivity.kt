package com.example.tadaplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
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
import androidx.camera.core.ExperimentalGetImage
import com.google.mlkit.vision.face.Face
import kotlin.math.ceil


class FaceDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFaceDetectionBinding
    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    private var currentStep = 0

    private val cameraXViewModel = viewModels<CameraXViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityFaceDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)


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
                    takePhoto(binding.graphicOverlay,face,imageProxy.image!!.cropRect)
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

    @SuppressLint("SuspiciousIndentation")
    private fun takePhoto(overlay: FaceBoxOverlay, face: Face, imageRect: Rect) {

        val bitmap = binding.previewView.bitmap ?: return
        val scaleX = overlay.width.toFloat() / imageRect.height().toFloat()
        val scaleY = overlay.height.toFloat() / imageRect.width().toFloat()
        val scale = scaleX.coerceAtLeast(scaleY)
        var izquierda : Float
        var derecha : Float
        var arriba: Float
        var abajo : Float
        
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
        // Obtener la imagen de la vista de la cámara (PreviewView)


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