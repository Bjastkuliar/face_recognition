/*
 * Copyright 2022 Alberto Nicoletti
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

package org.noi.face_recognition

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import org.noi.face_recognition.image.FrameAnalyser

/**
 * Creates a dialog that enables the user to input a name for labeling a face
 * whose preview will be shown within the dialog itself. It takes as parameters the
 * [embeddings] of the detected face and its [croppedBitmap] for the preview.
 * The fourth parameter is an optional [textView] that can be passed if there is
 * a view where to display the name inserted by the user.
 *
 * @author Alberto Nicoletti
 *
 * @param frameAnalyser passed in order to update the list of recognised faces
 */

class UnknownPersonDialogFragment(
    private val embeddings : FloatArray,
    private val croppedBitmap : Bitmap,
    private val frameAnalyser : FrameAnalyser,
    private val textView: TextView? = null
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
            if (textView != null) {
                textView.text=getString(R.string.result,name)
            }
            dialogInterface.dismiss()
        }
        builder.setView(dialogLayout)
        return builder.show()
    }
}
