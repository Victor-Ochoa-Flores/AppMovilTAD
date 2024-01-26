package com.example.tadaplication
import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tadaplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var cancellationSignal: CancellationSignal?= null
    private lateinit var binding: ActivityMainBinding
    private val authenticationCallback : BiometricPrompt.AuthenticationCallback
        get() =
            @RequiresApi(Build.VERSION_CODES.P)
            object : BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    notifyUser("Error al autenticar huella $errString")
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    notifyUser("Aunteticacion exitosa")
                    startActivity(Intent(this@MainActivity,SecretActivity::class.java))
                }
            }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    checkBiometricSupport()

        binding.btnAuthenticate.setOnClickListener(){
            val biometricPrompt = BiometricPrompt.Builder(this)
                .setTitle("Especifique su bloqueo de pantalla para acceder")
                .setSubtitle("Desbloquee la aplicación para continuar")
                .setNegativeButton("Cancel",this.mainExecutor,DialogInterface.OnClickListener{
                    dialog, which -> notifyUser("Autenticacion cancelada")
                }).build()
            biometricPrompt.authenticate(getCancellationSignal(),mainExecutor,authenticationCallback)
        }
    }

    private fun notifyUser (message: String){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show()
    }

    private fun getCancellationSignal (): CancellationSignal {
        cancellationSignal= CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Autenticacion fue cancelada por el usuario")
        }
        return  cancellationSignal as CancellationSignal
    }

    private fun checkBiometricSupport(): Boolean {
        val keyguardManager:KeyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure){
            notifyUser("FingerPrint no ha sido activado desde las configuraciones")
            return false
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_BIOMETRIC)!= PackageManager.PERMISSION_GRANTED){
            notifyUser("No se dieron los permisos a fingerprint")
            return false
        }

        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)){
            true
        }
        else true
    }
}

