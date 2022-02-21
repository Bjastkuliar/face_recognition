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

        val FACENET_512 = ModelInfo(
            "FaceNet-512" ,
            "face_net_512.tflite" ,
            0.3f ,
            23.56f ,
            512 ,
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

        val FACENET_512_QUANTIZED = ModelInfo(
            "FaceNet-512 Quantized" ,
            "face_net_512_int_quantized.tflite" ,
            0.3f ,
            23.56f ,
            512 ,
            160
        )

        val MOBILE_FACENET = ModelInfo(
            "MobileFaceNet",
            "mobile_face_net.tflite",
            0.0f,
            0.0f,
            128,
            0 //680 x 420
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