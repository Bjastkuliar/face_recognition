
package org.noi.face_recognition.analysis
import android.content.Context
import android.media.Image
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.noi.face_recognition.image.BitmapUtils
import org.noi.face_recognition.model.FaceNetModel
import org.noi.face_recognition.model.MaskDetectionModel

class ImageAnalyzer(context: Context, model: FaceNetModel) {
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode( FaceDetectorOptions.PERFORMANCE_MODE_FAST )
        .build()
    private val detector = FaceDetection.getClient(realTimeOpts)

//Used to determine whether the incoming frame should be dropped or processed.*

    private var isProcessing = false

//Store the face embeddings in a ( String , FloatArray ) ArrayList.
     //Where String -> name of the person and FloatArray Embedding of the face.

    private var faceList = ArrayList<Pair<String,FloatArray>>()

    private val maskDetectionModel = MaskDetectionModel( context )

    private var subject = FloatArray( model.embeddingDim )

    private val nameScoreHashmap = HashMap<String,ArrayList<Float>>()

    // <-------------- User controls --------------------------->

    // Use any one of the two metrics, "cosine" or "l2"
    private val metricToBeUsed = "l2"

    // Use this variable to enable/disable mask detection.
    private val isMaskDetectionOn = true

    fun analyze(image : Image){
        if ( isProcessing || faceList.size == 0 ) {
            image.close()
            return
        } else {
            isProcessing = true

            // Rotated bitmap for the FaceNet model
            /*val frameBitmap = BitmapUtils.imageToBitmap( image, )

            val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees )
            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    CoroutineScope( Dispatchers.Default ).launch {
                        runModel( faces , frameBitmap )
                    }
                }
                .addOnCompleteListener {
                    image.close()
                }*/
        }
    }
}
