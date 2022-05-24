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

package org.noi.face_recognition

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Picture
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.noi.face_recognition.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "Main"
private const val RTAG="MyTag"

class MainActivity : AppCompatActivity() {

    private var imageCapture  : ImageCapture? = null
    private lateinit var viewBinding : ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var queue:RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

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

        cameraExecutor = Executors.newSingleThreadExecutor()

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

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
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
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"

                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(baseContext.contentResolver,
                            output.savedUri!!
                        ))
                    } else {
                        MediaStore.Images.Media.getBitmap(baseContext.contentResolver, output.savedUri)
                    }
                    //send(bitmap)

                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
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

        val url = "http://10.11.145.3:5000/file_upload"

        val multipartRequest : CustomVolleyMultipartRequest = object : CustomVolleyMultipartRequest(
            Method.POST, url,
            {
                Log.d("response", responseToString(it.data))
            },{
                Log.e("GotError", "" + it.message)
            }
        ) {
            @Suppress("UNUSED_PARAMETER")
            override var byteData: Map<String, DataPart>
                get() = super.byteData
                set(value) {
                    val params: MutableMap<String, DataPart> = HashMap()
                    val imagename = System.currentTimeMillis()
                    params["image"] = DataPart("$imagename.png", getFileDataFromDrawable(bitmap))
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
}
