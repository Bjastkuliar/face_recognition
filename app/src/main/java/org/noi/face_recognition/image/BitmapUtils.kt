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

import android.content.ContentResolver
import android.content.Context
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import kotlin.math.ceil

// Helper class for operations on Bitmaps
class BitmapUtils {

    companion object {

        // Crop the given bitmap with the given rect.
        fun cropRectFromBitmap(source: Bitmap, rect: Rect): Bitmap {
            var width = rect.width()
            var height = rect.height()
            if ((rect.left + width) > source.width) {
                width = source.width - rect.left
            }
            if ((rect.top + height) > source.height) {
                height = source.height - rect.top
            }
            // Uncomment the below line if you want to save the input image.
            // BitmapUtils.saveBitmap( context , croppedBitmap , "source" )
            return Bitmap.createBitmap(source, rect.left, rect.top, width, height)
        }


        // Get the image as a Bitmap from given Uri
        // Source -> https://developer.android.com/training/data-storage/shared/documents-files#bitmap
        fun getBitmapFromUri( contentResolver : ContentResolver , uri: Uri): Bitmap {
            val parcelFileDescriptor: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        }


        // Rotate the given `source` by `degrees`.
        // See this SO answer -> https://stackoverflow.com/a/16219591/10878733
        fun rotateBitmap( source: Bitmap , degrees : Float ): Bitmap {
            val matrix = Matrix()
            matrix.postRotate( degrees )
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix , false )
        }


        // Flip the given `Bitmap` horizontally.
        // See this SO answer -> https://stackoverflow.com/a/36494192/10878733
        private fun flipBitmap( source: Bitmap ): Bitmap {
            val matrix = Matrix()
            matrix.postScale(-1f, 1f, source.width / 2f, source.height / 2f)
            return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        }


        // Use this method to save a Bitmap to the internal storage ( app-specific storage ) of your device.
        // To see the image, go to "Device File Explorer" -> "data" -> "data" -> "com.ml.quaterion.facenetdetection" -> "files"
        fun saveBitmap(context: Context, image: Bitmap, name: String) {
            val fileOutputStream = FileOutputStream(File( context.filesDir.absolutePath + "/$name.png"))
            image.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        }


        // Convert android.media.Image to android.graphics.Bitmap
        // See the SO answer -> https://stackoverflow.com/a/44486294/10878733
        fun imageToBitmap( image : Image , rotationDegrees : Int ): Bitmap {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val yuv = out.toByteArray()
            var output = BitmapFactory.decodeByteArray(yuv, 0, yuv.size)
            output = rotateBitmap( output , rotationDegrees.toFloat() )
            return flipBitmap( output )
        }


        // Convert the given Bitmap to NV21 ByteArray
        // See this comment -> https://github.com/firebase/quickstart-android/issues/932#issuecomment-531204396
        fun bitmapToNV21ByteArray(bitmap: Bitmap): ByteArray {
            val argb = IntArray(bitmap.width * bitmap.height )
            bitmap.getPixels(argb, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val yuv = ByteArray(bitmap.height * bitmap.width + 2 * ceil(bitmap.height / 2.0).toInt()
                    * ceil(bitmap.width / 2.0).toInt())
            encodeYUV420SP( yuv, argb, bitmap.width, bitmap.height)
            return yuv
        }

        private fun encodeYUV420SP(yuv420sp: ByteArray, argb: IntArray, width: Int, height: Int) {
            val frameSize = width * height
            var yIndex = 0
            var uvIndex = frameSize
            var r: Int
            var g: Int
            var b: Int
            var y: Int
            var u: Int
            var v: Int
            var index = 0
            for (j in 0 until height) {
                for (i in 0 until width) {
                    r = argb[index] and 0xff0000 shr 16
                    g = argb[index] and 0xff00 shr 8
                    b = argb[index] and 0xff shr 0
                    y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                    u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                    v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                    yuv420sp[yIndex++] = (if (y < 0) 0 else if (y > 255) 255 else y).toByte()
                    if (j % 2 == 0 && index % 2 == 0) {
                        yuv420sp[uvIndex++] = (if (v < 0) 0 else if (v > 255) 255 else v).toByte()
                        yuv420sp[uvIndex++] = (if (u < 0) 0 else if (u > 255) 255 else u).toByte()
                    }
                    index++
                }
            }
        }

    }

}