package org.noi.face_recognition.data

import android.content.Context
import android.util.Log
import java.io.*

const val SERIALIZED_DATA_FILENAME = "image_data"
const val TAG = "FileIO"

class FileIO(context: Context, debugMode : Boolean = false) {

    private var fileDirectory = context.filesDir
    private var debug = debugMode

    /**Saves the provided pair Arraylist to file**/
    fun saveSerializedImageData(data : ArrayList<Pair<String,FloatArray>>) {
        val serializedDataFile = File( fileDirectory , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Saving data at ${serializedDataFile.canonicalPath}")
        ObjectOutputStream( FileOutputStream( serializedDataFile )  ).apply {
            writeObject( data )
            flush()
            close()
        }
    }

    /**Loads the saved data from the data folder into the application**/
    /*The @Suppress tag is needed because we are reading data which we are sure is in the correct
    format, hence the "Unchecked cast" is not really unchecked*/
    @Suppress("UNCHECKED_CAST")
    fun loadSerializedImageData() : ArrayList<Pair<String,FloatArray>> {
        val serializedDataFile = File( fileDirectory , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Loading serialized data from ${serializedDataFile.absolutePath}")
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        return data
    }

    /**Prints all the data contained in the image_data to the debug console of LogCat**/
    /*The @Suppress tag is needed because we are reading data which we are sure is in the correct
    format, hence the "Unchecked cast" is not really unchecked*/
    @Suppress("UNCHECKED_CAST")
    fun printSerializedImageData(){
        val serializedDataFile = File( fileDirectory , SERIALIZED_DATA_FILENAME )
        Log.d(TAG,"Reading serialized data from ${serializedDataFile.canonicalPath}")
        val objectInputStream = ObjectInputStream( FileInputStream( serializedDataFile ) )
        val data = objectInputStream.readObject() as ArrayList<Pair<String,FloatArray>>
        objectInputStream.close()
        for(pair : Pair<String,FloatArray> in data){
            Log.d("Deserialized Data", pair.first+" ,"+pair.second.contentToString())
        }
    }

    /**Checks whether the image_data file exists in the data folder**/
    fun hasSerializedData() : Boolean{
        val file = File(fileDirectory, SERIALIZED_DATA_FILENAME)
        if(file.exists()){
            return true
        }
        return false
    }

    /**
     * Simple method that converts the data file stored in the
     * data folder to a text file for analysis, is disabled by default.
     * To enable specify the debugMode to true in the class initializer.**/
    /*The @Suppress tag is needed because we are reading data which we are sure is in the correct
    format, hence the "Unchecked cast" is not really unchecked*/
    @Suppress("UNCHECKED_CAST")
    fun copyDeserializedDataToTextFile(){

        if(debug){
            //read from serialized file
            val serializedDataFile = File(fileDirectory, SERIALIZED_DATA_FILENAME)
            Log.d(TAG, "Reading2 serialized data from ${serializedDataFile.absolutePath}")
            val objectInputStream = ObjectInputStream(FileInputStream(serializedDataFile))
            val data = objectInputStream.readObject() as ArrayList<Pair<String, FloatArray>>
            objectInputStream.close()

            //write to text file
            val deserializedDataFile = File(fileDirectory, "$SERIALIZED_DATA_FILENAME.txt")
            if (deserializedDataFile.exists()) {
                deserializedDataFile.delete()
            }
            Log.d(TAG, "Writing serialized data from ${serializedDataFile.absolutePath}")
            val fw = FileWriter(deserializedDataFile, true)
            data.forEach {
                fw.write(it.first)
                fw.write(System.lineSeparator())
                fw.write(it.second.joinToString())
                fw.write(System.lineSeparator())
            }
            fw.close()
        } else{
            Log.d(TAG, "Debug mode disabled by default")
        }
    }
}