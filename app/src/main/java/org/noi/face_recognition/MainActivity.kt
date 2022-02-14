package org.noi.face_recognition

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.lifecycle.LifecycleOwner
import org.noi.face_recognition.analysis.FrameAnalyzer
import org.noi.face_recognition.analysis.ImageAnalyzer
import org.noi.face_recognition.data.FileIO
import org.noi.face_recognition.databinding.ActivityMainBinding
import org.noi.face_recognition.model.FaceNetModel
import org.noi.face_recognition.model.Models
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

//TODO: image capture
//TODO: image capture passed to tflite model
//TODO: tflite returns embeddings
//TODO: embeddings confronted with already saved ones
//TODO: returns the label of most-similar one or Unknown

@ExperimentalGetImage
class MainActivity : AppCompatActivity() {

    private lateinit var viewBinding: ActivityMainBinding

    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture : ImageCapture? = null

    /*
    * Frame Analyzer analyzes a continuous stream of */
    private lateinit var frameAnalyzer : FrameAnalyzer
    private lateinit var imageAnalyzer : ImageAnalyzer

    private val fileIO = FileIO()

    private  lateinit var data : ArrayList<Pair<String,FloatArray>>

    /** Default Model is FaceNet**/
    //TODO: check if it is possible to switch to quantized (faster) models "dynamically"
    private val modelInfo = Models.FACE_NET

    private lateinit var faceNetModel : FaceNetModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* Remove the status bar to have a full screen experience
         * See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }


        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        faceNetModel = FaceNetModel( this , modelInfo , useGpu = true , useXNNPack = true)

        //frameAnalyzer = FrameAnalyzer(this,faceNetModel)
        imageAnalyzer = ImageAnalyzer(this,faceNetModel)

        if(fileIO.hasSerializedData(this)){
            data = fileIO.loadSerializedImageData(applicationContext)
        } else {
            Log.d(TAG,"Application has no serialized data")
        }

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

    /**
     * Method responsible for taking the picture when the button is pressed
     * The result picture is then passed onto the analyzer
     * **/
    @ExperimentalGetImage
    private fun takePicture(){
        val imageCapture = imageCapture ?: return
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.ITALIAN).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CameraX-Image")
            }
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()

        imageCapture.takePicture(
            getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback(){
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    //TODO: Process capture
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG,"Error while trying to capture the picture", exception)
                }
            }
        )
    }

    /*private fun ics(){

    }*/

    @ExperimentalGetImage
    private fun buttonHandler(){
        val textView = findViewById<TextView>(R.id.result)
        takePicture()
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

            imageCapture = ImageCapture.Builder().build()

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, getMainExecutor(this))
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