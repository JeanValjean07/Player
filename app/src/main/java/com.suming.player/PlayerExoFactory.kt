package com.suming.player

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi

@UnstableApi
class PlayerExoFactory private constructor(
    private val app: Application
) : ViewModelProvider.Factory {

    companion object {
        @Volatile
        private var INSTANCE: PlayerExoFactory? = null

        fun getInstance(app: Application): PlayerExoFactory =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlayerExoFactory(app).also { INSTANCE = it }
            }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PlayerExoViewModel(app) as T
    }
}