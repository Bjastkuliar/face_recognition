package org.noi.face_recognition

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import org.noi.face_recognition.data.FileIO
import org.noi.face_recognition.databinding.ActivityMainBinding
import org.noi.face_recognition.image.FrameAnalyser
import org.noi.face_recognition.model.FaceNetModel
import org.noi.face_recognition.model.Models
import java.util.concurrent.Executors

/**
 * This class is responsible for initializing and loading all components of the application itself.
 * It makes use of fileIO for loading/saving data, the image package handles all image-processing requests
 * and finally the model package handles everything related to the model. Interaction with the user
 * is handled through []
 */

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var previewView : PreviewView
    private lateinit var frameAnalyser  : FrameAnalyser
    private lateinit var faceNetModel : FaceNetModel
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>
    private lateinit var fileIO : FileIO
    private lateinit var textView: TextView
    private lateinit var button : Button
    private lateinit var viewBinding : ActivityMainBinding

    // You may the change the models here.
    // Use the model configs in Models.kt
    // Default is Models.FACENET ; Quantized models are faster
    private val modelInfo = Models.FACENET_QUANTIZED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityMainBinding.inflate(layoutInflater)

        // Remove the status bar to have a full screen experience
        // See this answer on SO -> https://stackoverflow.com/a/68152688/10878733
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }
        setContentView(viewBinding.root)

        // Implementation of CameraX preview and the Feedback TextView
        previewView = findViewById( R.id.preview_view )
        textView = findViewById(R.id.textView)
        textView.text=getString(R.string.result,"Unknown")
        button = findViewById(R.id.button)

        faceNetModel = FaceNetModel( this , modelInfo , useGpu = true , useXNNPack = true)
        frameAnalyser = FrameAnalyser(this, faceNetModel, textView, supportFragmentManager)


        // We'll only require the CAMERA permission from the user.
        // For scoped storage, particularly for accessing documents, we won't require WRITE_EXTERNAL_STORAGE or
        // READ_EXTERNAL_STORAGE permissions. See https://developer.android.com/training/data-storage
        if ( !allPermissionsGranted() ) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
        else {
            startCameraPreview()
        }
        //TODO: Remove debugMode
        fileIO = FileIO(this,true)
        if(fileIO.hasSerializedData()){
            frameAnalyser.faceList=fileIO.loadSerializedImageData()
            Log.d(TAG, "Serialized data loaded.")
        } else {
            Log.d(TAG, "No serialized data was found.")
        }

        button.setOnClickListener {
            frameAnalyser.takePicture()
        }
    }

    // Attach the camera stream to the PreviewView.
    private fun startCameraPreview() {
        cameraProviderFuture = ProcessCameraProvider.getInstance( this )
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider) },
            ContextCompat.getMainExecutor(this) )
    }

    private fun bindPreview(cameraProvider : ProcessCameraProvider) {
        val preview : Preview = Preview.Builder().build()
        val cameraSelector : CameraSelector = CameraSelector.Builder()
            .requireLensFacing( CameraSelector.LENS_FACING_FRONT )
            .build()
        preview.setSurfaceProvider( previewView.surfaceProvider )
        val imageFrameAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size( 480, 640 ) )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageFrameAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), frameAnalyser )
        cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview , imageFrameAnalysis  )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCameraPreview()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    /**Checks whether all permissions required were granted,
     * see companion object for the permissions we are requiring**/
    private fun allPermissionsGranted()= REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        fileIO.saveSerializedImageData(frameAnalyser.faceList)
        fileIO.copyDeserializedDataToTextFile()
        super.onDestroy()
    }

    companion object{
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
