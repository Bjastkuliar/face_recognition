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
package org.noi.face_recognition.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "FaceNetModel"

/**
 * Initializes the [model] and performs operations on the input.
 */
class FaceNetModel(context : Context,
                   var model : ModelInfo,
                   useGpu : Boolean,
                   useXNNPack : Boolean) {

    // Input image size for FaceNet model.
    private val imgSize = model.inputDims

    // Output embedding size
    val embeddingDim = model.outputDims

    private var interpreter : Interpreter
    private val imageTensorProcessor = ImageProcessor.Builder()
        .add( ResizeOp( imgSize , imgSize , ResizeOp.ResizeMethod.BILINEAR ) )
        .add( StandardizeOp() )
        .build()

    init {
        // Initialize TFLiteInterpreter
        val interpreterOptions = Interpreter.Options().apply {
            // Add the GPU Delegate if supported.
            // See -> https://www.tensorflow.org/lite/performance/gpu#android
            if ( CompatibilityList().isDelegateSupportedOnThisDevice ) {
                if ( useGpu ) {
                    addDelegate( GpuDelegate( CompatibilityList().bestOptionsForThisDevice ))
                }
            }
            else {
                // Number of threads for computation
                setNumThreads( 4 )
            }
            setUseXNNPACK( useXNNPack )
        }
        interpreter = Interpreter(FileUtil.loadMappedFile(context, model.assetsFilename ) , interpreterOptions )
        Log.d(TAG,"Using ${model.name} model.")
    }

    /**
     * Gets the embedding of a face from the given [image] using the specified [model]
     */
    fun getFaceEmbedding( image : Bitmap ) : FloatArray {
        return runFaceNet( convertBitmapToBuffer( image ))[0]
    }

    /**
     * Runs the [model] on the [inputs]
     */
    private fun runFaceNet(inputs: Any): Array<FloatArray> {
        val t1 = System.currentTimeMillis()
        val faceNetModelOutputs = Array( 1 ){ FloatArray( embeddingDim ) }
        interpreter.run( inputs, faceNetModelOutputs )
        Log.i( "Performance" , "${model.name} Inference Speed in ms : ${System.currentTimeMillis() - t1}")
        return faceNetModelOutputs
    }



    /**
     * Resizes the given [image] to a fit the [imageTensorProcessor] input and
     * converts it into a [ByteBuffer]
     */
    private fun convertBitmapToBuffer( image : Bitmap) : ByteBuffer {
        return imageTensorProcessor.process( TensorImage.fromBitmap( image ) ).buffer
    }




    /**
     * Operator to perform standardization.
     */
    // x' = ( x - mean ) / std_dev
    class StandardizeOp : TensorOperator {

        @Suppress("SimplifiableCallChain")
        override fun apply(p0: TensorBuffer?): TensorBuffer {
            val pixels = p0!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt( pixels.map{ pi -> ( pi - mean ).pow( 2 ) }.sum() / pixels.size.toFloat() )
            std = max( std , 1f / sqrt( pixels.size.toFloat() ))
            for ( i in pixels.indices ) {
                pixels[ i ] = ( pixels[ i ] - mean ) / std
            }
            val output = TensorBufferFloat.createFixedSize( p0.shape , DataType.FLOAT32 )
            output.loadArray( pixels )
            return output
        }

    }

}