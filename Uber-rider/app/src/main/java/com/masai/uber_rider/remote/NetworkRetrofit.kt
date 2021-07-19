package com.masai.uber_rider.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkRetrofit {
    val instance: Retrofit? = null

    fun getInstanceRe(): Retrofit? {
        return NetworkRetrofit.instance
            ?: Retrofit.Builder()
                .baseUrl("https://maps.googleapis.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
    }
}