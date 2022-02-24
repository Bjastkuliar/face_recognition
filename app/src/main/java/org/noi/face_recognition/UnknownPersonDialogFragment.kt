package org.noi.face_recognition

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import org.noi.face_recognition.image.FrameAnalyser

class UnknownPersonDialogFragment(
    private val embeddings : FloatArray,
    private val croppedBitmap : Bitmap,
    private val frameAnalyser : FrameAnalyser,
    private val textView: TextView
    ) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog{
        val builder = AlertDialog.Builder(requireContext())
        val dialogLayout = layoutInflater.inflate(R.layout.unknown_person_dialog,null)
        val picture = dialogLayout.findViewById<ImageView>(R.id.dlg_image)
        picture.setImageBitmap(croppedBitmap)
        val input = dialogLayout.findViewById<EditText>(R.id.dlg_input)

        builder.setPositiveButton("OK"){ dialogInterface: DialogInterface, _: Int ->
            val name = input.text.toString()
            if(name.isEmpty()){
                return@setPositiveButton
            }
            val pair = Pair(name,embeddings)
            frameAnalyser.faceList.add(pair)
            textView.text=getString(R.string.result,name)
            dialogInterface.dismiss()
        }
        builder.setView(dialogLayout)
        return builder.show()
    }
}
