package org.noi.face_recognition.data

import android.content.Context
import android.util.Log
import java.io.*

private const val SERIALIZED_DATA_FILENAME = "image_data"
private const val TAG = "FileIO"

/**
 * Utility class for loading/unloading file data from-to
 * the activity itself, acts as a "gateway class" in order
 * to easily swap-in/out the process of saving-loading data.
 * making it easy to attach either a Room implementation or
 * an external database.
 *
 * @author Alberto Nicoletti
 * @param context the context of the main activity
 * @param debugMode default set to false, enables copySerializedDataToTextFile
 **/

class FileIO(context: Context, debugMode : Boolean = false) {

    private val fileDirectory = context.filesDir
    private val debug = debugMode
    private var assets = false
    private val assetManager = context.assets

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
    fun loadSerializedImageData(): ArrayList<Pair<String,FloatArray>> {
        var data: ArrayList<Pair<String, FloatArray>>

        try {
            //image_data exists within the application asset folder
            val assetStream = assetManager.open(SERIALIZED_DATA_FILENAME)
            Log.d(TAG,"Found a serialized data within the assets")
            val objectInputStream = ObjectInputStream(assetStream)
            data = objectInputStream.readObject() as ArrayList<Pair<String, FloatArray>>
            objectInputStream.close()
            assets = true
        } catch ( e : IOException){
            //image_data is loaded from internal application data
            Log.d(TAG,fileDirectory.absolutePath)
            val serializedDataFile = File( fileDirectory , SERIALIZED_DATA_FILENAME )
            Log.d(TAG, "Loading serialized data from ${serializedDataFile.canonicalPath}")
            val objectInputStream2 = ObjectInputStream(FileInputStream(serializedDataFile))
            data = objectInputStream2.readObject() as ArrayList<Pair<String, FloatArray>>
            objectInputStream2.close()
        }
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

    /**Checks whether the image_data file exists in the data folder or among the assets**/
    fun hasSerializedData() : Boolean{
        val file = File(fileDirectory, SERIALIZED_DATA_FILENAME)
        val assetList = assetManager.list("")
        if(file.exists()|| assetList?.contains(SERIALIZED_DATA_FILENAME) != false){
            return true
        }
        return false
    }

    /**
     * Simple method that converts the data file stored in the
     * data folder to a text file for analysis, is disabled by default.
     * To enable specify the debugMode to true in the class initializer.
     * Since it copies the given file to a text one, it might be wiser to call
     * this method after the saveSerializedImageData method.**/
    /*The @Suppress tag is needed because we are reading data which we are sure is in the correct
    format, hence the "Unchecked cast" is not really unchecked*/
    @Suppress("UNCHECKED_CAST")
    fun copyDeserializedDataToTextFile(){

        if(debug){
            val data : ArrayList<Pair<String,FloatArray>>
            //read from serialized file
                if(assets){
                    val assetStream = assetManager.open(SERIALIZED_DATA_FILENAME)
                    Log.d(TAG,"Reading data from the assets")
                    val objectInputStream = ObjectInputStream(assetStream)
                    data = objectInputStream.readObject() as ArrayList<Pair<String, FloatArray>>
                    objectInputStream.close()
                } else {
                    val serializedDataFile = File(fileDirectory, SERIALIZED_DATA_FILENAME)
                    Log.d(TAG, "Reading2 serialized data from ${serializedDataFile.absolutePath}")
                    val objectInputStream = ObjectInputStream(FileInputStream(serializedDataFile))
                    data = objectInputStream.readObject() as ArrayList<Pair<String, FloatArray>>
                    objectInputStream.close()
                }

            //write to text file
            val deserializedDataFile = File(fileDirectory, "$SERIALIZED_DATA_FILENAME.txt")
            if (deserializedDataFile.exists()) {
                deserializedDataFile.delete()
            }
            Log.d(TAG, "Writing serialized data to  ${deserializedDataFile.absolutePath}")
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