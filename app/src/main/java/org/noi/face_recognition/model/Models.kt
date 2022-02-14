package org.noi.face_recognition.model

import java.io.File

/**
 * Template for the various FaceNet models
 * @param outputDims the size of the vector outputted by the model
 * @param inputDims the size of the image input**/
data class ModelInfo(
    val name : String ,
    val assetsFilename : String ,
    val cosineThreshold : Float ,
    val l2Threshold : Float ,
    val outputDims : Int ,
    val inputDims : Int )

class Models {

    companion object {

        val FACE_NET = ModelInfo(
            "FaceNet" ,
            "face_net.tflite" ,
            0.4f ,
            10f ,
            128 ,
            160
        )

        val FACE_NET_512 = ModelInfo(
            "FaceNet-512" ,
            "face_net_512.tflite" ,
            0.3f ,
            23.56f ,
            512 ,
            160
        )

        val FACE_NET_QUANTIZED = ModelInfo(
            "FaceNet Quantized" ,
            "face_net_int_quantized.tflite" ,
            0.4f ,
            10f ,
            128 ,
            160
        )

        val FACE_NET_512_QUANTIZED = ModelInfo(
            "FaceNet-512 Quantized" ,
            "face_net_512_int_quantized.tflite" ,
            0.3f ,
            23.56f ,
            512 ,
            160
        )

        val MOBILE_FACE_NET = ModelInfo(
            "Mobile FaceNet",
            "mobile_face_net.tflite",
            0.0f ,
            0.0f,
            128 ,
            0 //640x480
        )


    }

}