package com.suming.player

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

object NetworkClient {

    const val githubRepository = "JeanValjean07/Player"


    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()




    fun request(request: Request): Response {
        return client.newCall(request).execute()
    }





}