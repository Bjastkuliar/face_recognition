

package org.noi.face_recognition.model

class Models {

    companion object {
        val FACENET = ModelInfo(
            "FaceNet" ,
            "face_net.tflite" ,
            0.4f ,
            10f ,
            128 ,
            160
        )

        val FACENET_QUANTIZED = ModelInfo(
            "FaceNet Quantized" ,
            "face_net_int_quantized.tflite" ,
            0.4f ,
            10f ,
            128 ,
            160
        )
    }
}

data class ModelInfo(
    val name : String ,
    val assetsFilename : String ,
    val cosineThreshold : Float ,
    val l2Threshold : Float ,
    val outputDims : Int ,
    val inputDims : Int )