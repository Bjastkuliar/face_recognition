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

package org.noi.face_recognition.data

/**
 * Utility interface designed in order to simplify the redesign of the IOclass attached to the project
 * @author Alberto Nicoletti
 */
interface IOTemplate {

    /**Saves the provided [data] to file**/
    fun saveSerializedImageData(data : ArrayList<Pair<String,FloatArray>>)

    /**Loads the saved data from the data folder into the application**/
    fun loadSerializedImageData() : ArrayList<Pair<String,FloatArray>>

    /**Checks whether the image_data file exists in the data folder or among the assets**/
    fun hasSerializedImageData() : Boolean

}