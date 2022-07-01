package org.noi.androidclient.configuration

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface API {
    @Multipart
    @POST("fileUpload/")
    fun uploadImage(@Part image: MultipartBody.Part): Call<ResponseBody>  // POST request to upload an image from storage
}