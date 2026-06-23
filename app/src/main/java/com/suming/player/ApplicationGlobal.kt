package com.suming.player

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class ApplicationGlobal : Application() {

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        PlayerSingleton.setContext(this)

    }

}