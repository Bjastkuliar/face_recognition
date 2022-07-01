package org.noi.androidclient_mobile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import okhttp3.*
import org.json.JSONObject
import org.noi.androidclient.configuration.API
import org.noi.androidclient_mobile.configuration.RetrofitClient
import org.noi.androidclient_mobile.databinding.ActivityMainBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.UnsupportedEncodingException
import java.net.UnknownHostException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var outputDirectory: File

    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var preview: Preview
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider

    private lateinit var imageCapture  : ImageCapture
    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

    }

    @androidx.camera.core.ExperimentalGetImage
    override fun onStart() {
        super.onStart()
        if(allPermissionsGranted()){
            setUpCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        window.decorView.windowInsetsController!!
            .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

        outputDirectory = applicationContext.filesDir

        viewBinding.button.setOnClickListener { takePicture() }

        viewBinding.quit.setOnClickListener{
            onStop()
            onDestroy()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setUpCamera()
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

    companion object{
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET
            ).toTypedArray()

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    override fun onStop() {
        super.onStop()
        cameraExecutor.shutdown()
    }

    /** Initialize CameraX, and prepare to bind the camera use cases**/
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(baseContext)
        cameraProviderFuture.addListener({

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(baseContext))
    }

    /** Declare and bind preview, capture and analysis use cases**/
    private fun bindCameraUseCases() {

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = baseContext.resources.displayMetrics
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        // CameraProvider
        val cameraProvider = cameraProvider

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            .build()

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        // A variable number of use-cases can be passed here -
        // camera provides access to CameraControl & CameraInfo
        camera = cameraProvider.bindToLifecycle(
            this, cameraSelector, preview, imageCapture)

        // Attach the viewfinder's surface provider to preview use case
        preview.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
    }

    private fun takePicture() {

        // Create output file to hold the image
        val photoFile = File(outputDirectory, "image.jpg")

        if(photoFile.exists()){
            if(photoFile.delete()){
                Log.d(TAG,"Removed old image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d(TAG, "Photo capture succeeded: $savedUri")

                    //Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)

                    // If the folder selected is an external media directory, this is
                    // unnecessary but otherwise other apps will not be able to access our
                    // images unless we scan them using [MediaScannerConnection]

                    //uploadImage(savedUri)
                    uploadImage(File(savedUri!!.path!!))
                }
            })
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    // Upload the image to the remote database
    fun uploadImage(savedUri: Uri) {
        val imageFile = File(savedUri.path!!) // Create a file using the absolute path of the image
        Log.d(TAG,"Absolute Path: ${imageFile.absolutePath}")
        val reqBody = RequestBody.create(MultipartBody.FORM, imageFile)
        Log.d(TAG, "Request Body Length: $reqBody")
        Log.d(TAG, "Request Body Type: ${reqBody.contentType()}")
        Log.d(TAG, "Request Body Length: ${reqBody.contentLength()}")
        val partImage = MultipartBody.Part.createFormData("file", imageFile.name, reqBody)
        Log.d(TAG, "PartImage: $partImage")
        Log.d(TAG, "PartImage Headers: ${partImage.headers()}")
        Log.d(TAG, "PartImage Body: ${partImage.body()}")
        val api: API = RetrofitClient().getInstance().getAPI()
        val upload: Call<ResponseBody> = api.uploadImage(partImage)

        upload.enqueue(object : Callback<ResponseBody?> {
            override fun onResponse(call: Call<ResponseBody?>, response: Response<ResponseBody?>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Image Uploaded", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Request failed", Toast.LENGTH_SHORT).show()
                Log.d(TAG,call.toString(),t)
            }
        })
    }

    fun uploadImage(file: File?): JSONObject? {
        try {
            val MEDIA_TYPE_PNG = MediaType.parse("image/png")
            val req: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("userid", "8457851245")
                .addFormDataPart(
                    "file",
                    "image.png",
                    RequestBody.create(MEDIA_TYPE_PNG, file!!)
                )
                .build()
            val request: Request = Request.Builder()
                .url("http://192.168.1.3:5000/fileUpload/")
                .post(req)
                .build()
            val client = OkHttpClient()
            val response: okhttp3.Response = client.newCall(request).execute()
            Log.d("response", "uploadImage:" + (response.body()?.string() ?: "Null response body"))
            return JSONObject(response.body()?.string() ?: "empty response")
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Error: " + e.localizedMessage)
        } catch (e: UnsupportedEncodingException) {
            Log.e(TAG, "Error: " + e.localizedMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Other Error: " + e.localizedMessage)
        }
        return null
    }
}