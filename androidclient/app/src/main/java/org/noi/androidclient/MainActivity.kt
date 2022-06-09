/*
 * Copyright 2022 Alberto Nicoletti
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.noi.androidclient

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.robotemi.sdk.Robot
import com.robotemi.sdk.listeners.OnRobotReadyListener
import org.noi.androidclient.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


private const val TAG = "Main"

class MainActivity : AppCompatActivity(), OnRobotReadyListener {

    private lateinit var robot : Robot

    private var imageCapture  : ImageCapture? = null
    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var queue:RequestQueue

    @androidx.camera.core.ExperimentalGetImage
    override fun onStart() {
        super.onStart()

        robot.addOnRobotReadyListener(this)
        if(allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController!!
                .hide( WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        }
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        }

        queue = Volley.newRequestQueue(applicationContext)

        viewBinding.button.setOnClickListener { takePhoto()}

        viewBinding.quit.setOnClickListener{
            onStop()
            onDestroy()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

    }

    override fun onRobotReady(isReady: Boolean) {
        if (isReady) {
            refreshTemiUi()
        }
    }

    override fun onRestart() {
        super.onRestart()
        refreshTemiUi()
    }

    private fun refreshTemiUi() {
        try {
            val activityInfo = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)
            Robot.getInstance().onStart(activityInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        robot = Robot.getInstance()
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

    }

    // Attach the camera stream to the PreviewView.
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            /*val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
                }*/

            imageCapture = ImageCapture.Builder()
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    if(image.image!== null){
                        send(toBitmap(image.image!!))
                    } else {
                        Log.e(TAG,"Image is null")
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

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
            ).apply {
                if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

    private fun send(bitmap: Bitmap){

        var imageName = ""

        val url = "http://10.11.145.3:5000/recognition/${imageName}.png"

        val multipartRequest : CustomVolleyMultipartRequest = object : CustomVolleyMultipartRequest(
            Method.POST, url,
            {
                Log.d("response", responseToString(it.data))
            },{
                Log.e("GotError", "" + it.message)
            }
        ) {
            override fun getByteData(): MutableMap<String, DataPart> {
                val params: MutableMap<String, DataPart> = HashMap()
                imageName = System.currentTimeMillis().toString()
                params["image"] = DataPart("$imageName.png", getFileDataFromDrawable(bitmap))
                return params
            }
        }

        queue.add(multipartRequest)

    }

    fun getFileDataFromDrawable(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun responseToString(array: ByteArray): String {
        val builder = StringBuilder()
        for (aByte in array) {
            builder.append(aByte.toInt()).append(" ")
        }
        return builder.toString()
    }

    override fun onStop() {
        super.onStop()
        cameraExecutor.shutdown()
        queue.cancelAll(TAG)
    }

    private fun toBitmap(image: Image): Bitmap {
        val planes: Array<Image.Plane> = image.planes
        val yBuffer: ByteBuffer = planes[0].buffer
        val uBuffer: ByteBuffer = planes[1].buffer
        val vBuffer: ByteBuffer = planes[2].buffer
        val ySize: Int = yBuffer.remaining()
        val uSize: Int = uBuffer.remaining()
        val vSize: Int = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}
