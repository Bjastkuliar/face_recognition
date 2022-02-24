/*
 * Copyright 2021 Shubham Panchal
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
package org.noi.face_recognition.image

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.TextView
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.fragment.app.FragmentManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.noi.face_recognition.R
import org.noi.face_recognition.UnknownPersonDialogFragment
import org.noi.face_recognition.model.FaceNetModel
import org.noi.face_recognition.model.MaskDetectionModel
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "FrameAnalyser"


// Analyser class to process frames and produce detections.
class FrameAnalyser(
    private val context: Context,
    private val model: FaceNetModel,
    private val textView: TextView,
    private val fragmentManager: FragmentManager
) : ImageAnalysis.Analyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
            .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

    private val nameScoreHashmap = HashMap<String,ArrayList<Float>>()
    private var subject = FloatArray( model.embeddingDim )

    // Used to determine whether the incoming frame should be dropped or processed.
    private var isProcessing = false

    // Store the face embeddings in a ( String , FloatArray ) ArrayList.
    // Where String -> name of the person and FloatArray -> Embedding of the face.
    var faceList = ArrayList<Pair<String,FloatArray>>()

    private val maskDetectionModel = MaskDetectionModel( context )

    // <-------------- User controls --------------------------->

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "l2"

    // Use this variable to enable/disable mask detection.
    private val isMaskDetectionOn = true

    // <-------------------------------------------------------->

    private var takePicture = true

    var addUnknown : UnknownPerson? = null

    /**
     * Implementation method of the ImageAnalysis class, which prepares the [image]
     * for analysis when the user clicks the button. Then hands everything to the runModel function
     * which asyncronously runs face recognition on the bitmap.
     */
    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(image: ImageProxy) {
        // If the previous frame is still being processed, then skip this frame
        if ( takePicture || isProcessing || faceList.size == 0 ) {
            image.close()
            return
        }
        else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            val frameBitmap =
                BitmapUtils.imageToBitmap(image.image!!, image.imageInfo.rotationDegrees)

            // Configure frameHeight and frameWidth for output2overlay transformation matrix.
            val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope( Dispatchers.Default ).launch {
                        runModel( faces , frameBitmap )
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }
        }
    }


    /**
     * Performs face detection on the given [cameraFrameBitmap], cropping it accordingly to the model
     * used. Runs the model on the cropped bitmap and computes the similarity between the detected [faces]
     * and the stored ones, assessing who it is according to the settings provided (either cosine similarity
     * or l2 norm).
     */
    private suspend fun runModel(faces : List<Face>, cameraFrameBitmap : Bitmap ){
        withContext( Dispatchers.Default ) {
            for (face in faces) {
                try {
                    /* Crop the frame using face.boundingBox.
                     Convert the cropped Bitmap to a ByteBuffer.
                     Finally, feed the ByteBuffer to the FaceNet model.*/
                    val croppedBitmap =
                        BitmapUtils.cropRectFromBitmap(cameraFrameBitmap, face.boundingBox)
                    subject = model.getFaceEmbedding( croppedBitmap )

                    // Perform face mask detection on the cropped frame Bitmap.
                    var maskLabel = ""
                    if ( isMaskDetectionOn ) {
                        maskLabel = maskDetectionModel.detectMask( croppedBitmap )
                    }

                    // Continue with the recognition if the user is not wearing a face mask
                    if (maskLabel == maskDetectionModel.noMask) {
                        /* Perform clustering ( grouping )
                         Store the clusters in a HashMap. Here, the key would represent the 'name'
                         of that cluster and ArrayList<Float> would represent the collection of all
                         L2 norms/ cosine distances.*/
                        for ( i in 0 until faceList.size ) {
                            /* If this cluster ( i.e an ArrayList with a specific key ) does not exist,
                             initialize a new one.*/
                            if ( nameScoreHashmap[ faceList[ i ].first ] == null ) {
                                // Compute the L2 norm and then append it to the ArrayList.
                                val p = ArrayList<Float>()
                                if ( metricToBeUsed == "cosine" ) {
                                    p.add( cosineSimilarity( subject , faceList[ i ].second ) )
                                }
                                else {
                                    p.add( l2Norm( subject , faceList[ i ].second ) )
                                }
                                nameScoreHashmap[ faceList[ i ].first ] = p
                            }
                            // If this cluster exists, append the L2 norm/cosine score to it.
                            else {
                                if ( metricToBeUsed == "cosine" ) {
                                    nameScoreHashmap[ faceList[ i ].first ]?.add( cosineSimilarity( subject , faceList[ i ].second ) )
                                }
                                else {
                                    nameScoreHashmap[ faceList[ i ].first ]?.add( l2Norm( subject , faceList[ i ].second ) )
                                }
                            }
                        }

                        // Compute the average of all scores norms for each cluster.
                        val avgScores = nameScoreHashmap.values.map{ scores -> scores.toFloatArray().average() }
                        Log.d(TAG, "Average score for each user : $nameScoreHashmap" )

                        val names = nameScoreHashmap.keys.toTypedArray()
                        nameScoreHashmap.clear()

                        // Calculate the minimum L2 distance from the stored average L2 norms.
                        val bestScoreUserName: String = if ( metricToBeUsed == "cosine" ) {
                            // In case of cosine similarity, choose the highest value.
                            @Suppress("SimplifiableCallChain")
                            if ( avgScores.maxOrNull()!! > model.model.cosineThreshold ) {
                                names[ avgScores.indexOf( avgScores.maxOrNull()!! ) ]
                            }
                            else {
                                "Unknown"
                            }
                        } else {
                            // In case of L2 norm, choose the lowest value.
                            if ( avgScores.minOrNull()!! > model.model.l2Threshold ) {
                                "Unknown"
                            }
                            else {
                                names[ avgScores.indexOf( avgScores.minOrNull()!! ) ]
                            }
                        }
                        Log.d(TAG, "Person identified as $bestScoreUserName" )
                        updateTextView(bestScoreUserName)
                        takePicture = true

                        /*Initializes the popup dialog which allows the user to input a new face
                        * into the knowledge base*/
                        if(bestScoreUserName == "Unknown"){
                            createAndShowDialog(croppedBitmap)
                            Log.d("addUnknown","Added unknown person")
                        }
                    }

                }
                catch ( e : Exception ) {
                    // If any exception occurs with this box and continue with the next boxes.
                    Log.e( "Model" , "Exception in FrameAnalyser : ${e.message}" )
                    continue
                }
            }
            withContext( Dispatchers.Main ) {
                isProcessing = false
            }
        }
    }


    /**
     * Computes the L2 norm of ( [x2] - [x1] )
     */
    private fun l2Norm(x1 : FloatArray, x2 : FloatArray ) : Float {
        return sqrt( x1.mapIndexed{ i , xi -> (xi - x2[ i ]).pow( 2 ) }.sum() )
    }


    /**
     * Computes the cosine of the angle between [x1] and [x2].
     */
    @Suppress("SimplifiableCallChain")
    private fun cosineSimilarity( x1 : FloatArray , x2 : FloatArray ) : Float {
        val mag1 = sqrt( x1.map { it * it }.sum() )
        val mag2 = sqrt( x2.map { it * it }.sum() )
        val dot = x1.mapIndexed{ i , xi -> xi * x2[ i ] }.sum()
        return dot / (mag1 * mag2)
    }

    /**
     * Function which updates the feedback textView with
     * the given [name].
     */
    private fun updateTextView(name : String){
        textView.text = context.getString(R.string.result,name)
    }

    /**
     * Very simple function that enables the frame analyzer to process one
     * (and only one) image capture, implementing the "button behaviour" for
     * the stream of pictures that is the CameraX's imageProxy
     */
    fun takePicture() {
        takePicture = false
    }

    /**
     * Simple internal data class for storing the information about the unknown person.
     * This is needed in order to provide information to the unknown person dialog.
     *
     * @param embeddings the model output
     * @param bitmap the image that has been analyzed
     */

    @Suppress("ArrayInDataClass")
    data class UnknownPerson(val embeddings : FloatArray, val bitmap: Bitmap)

    /**
     * this method is called whenever an unknown person is found,
     * it prompts a dialog with a [preview] of the detected face and
     * allows to input the name of the shown face.
     */
    private fun  createAndShowDialog(preview: Bitmap){
        val dialog = UnknownPersonDialogFragment(subject,preview,this,textView)
        dialog.show(fragmentManager, "UnknownPersonDialogFragment")
    }
}