package org.noi.face_recognition.data

import android.content.Context
import android.util.Log
import org.noi.face_recognition.model.TAG
import java.io.*

class FileIO {

    private var hasSerializedData = false

    fun saveSerializedImageData(data : ArrayList<Pair<String,FloatArray>>, context: Context ) {
        val serializedDataFile = File( context.filesDir , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Saving data at ${serializedDataFile.canonicalPath}")
        ObjectOutputStream( FileOutputStream( serializedDataFile )  ).apply {
            writeObject( data )
            flush()
            close()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun loadSerializedImageData(context: Context) : ArrayList<Pair<String,FloatArray>> {
        val serializedDataFile = File( context.filesDir , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Loading serialized data from ${serializedDataFile.canonicalPath}")
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        return data
    }

    @Suppress("UNCHECKED_CAST")
    fun printSerializedImageData(context: Context){
        val serializedDataFile = File( context.filesDir , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Reading serialized data from ${serializedDataFile.canonicalPath}")
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        for(pair : Pair<String,FloatArray> in data){
            Log.d("Deserialized Data", pair.first+" ,"+pair.second.contentToString())
        }
    }

    fun hasSerializedData(context: Context) : Boolean{
        val file = File(context.filesDir, SERIALIZED_DATA_FILENAME)
        if(file.exists()){
            return true
        }
        return false
    }

    companion object{
        const val SERIALIZED_DATA_FILENAME = "image_data"

    }
}