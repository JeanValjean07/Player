package com.suming.player.PlayerImplements

interface PlayerCallBack {

    fun onPrepared()
    fun onCompletion()
    fun onError(error: Throwable)
    fun onBuffering(percent: Int)
    fun onPlayingChanged(isPlaying: Boolean)

    fun onIdle()

}