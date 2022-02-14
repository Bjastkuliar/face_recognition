package org.noi.face_recognition

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.noi.face_recognition.databinding.ActivityMainBinding
import org.noi.face_recognition.model.FaceNetModel
import org.noi.face_recognition.model.Models
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//TODO: image capture
//TODO: image capture passed to tflite model
//TODO: tflite returns embeddings
//TODO: embeddings confronted with already saved ones
//TODO: returns the label of most-similar one or Unknown

class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    /** Default Model is FaceNet**/
    //TODO: check if it is possible to switch to quantized (faster) models "dynamically"
    private val modelInfo = Models.FACE_NET

    private lateinit var faceNetModel : FaceNetModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //hides systemBar
        //TODO: not working properly
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.R){
            val windowInsetsController = window.decorView.windowInsetsController
            if(windowInsetsController == null){
                return
            } else {
                windowInsetsController.hide(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }


        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        faceNetModel = FaceNetModel( this , modelInfo , useGpu = true , useXNNPack = true)

        //request permissions
        if(allPermissionsGranted()){
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener { buttonHandler() }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun buttonHandler(){
        Toast.makeText(this,"To be implemented",Toast.LENGTH_SHORT).show()
        val textView = findViewById<TextView>(R.id.result)
        textView.text = getString(R.string.result, "Unknown")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview, to be removed in the final prototype
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.preview.surfaceProvider)
                }

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted()= REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object{
        private const val  TAG = "CameraXApp"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO
            ).apply {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

}