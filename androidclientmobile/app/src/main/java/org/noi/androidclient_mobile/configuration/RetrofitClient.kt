package org.noi.androidclient_mobile.configuration

import org.noi.androidclient.configuration.API
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val BASE_URL = "http://192.168.1.3:5000/"
class RetrofitClient {
    private var mInstance: RetrofitClient? = null
    var retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Synchronized
    fun getInstance(): RetrofitClient {

        if(mInstance == null){
            mInstance = RetrofitClient()
        }
        return mInstance!!
    }

    fun getAPI(): API {
        return retrofit.create(API::class.java)
    }
}