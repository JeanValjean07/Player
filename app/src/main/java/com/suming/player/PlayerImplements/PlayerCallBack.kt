package com.suming.player.PlayerImplements

interface PlayerCallBack {

    fun onPrepared()
    fun onReady()
    fun onMediaEnd()
    fun onError(error: Throwable)
    fun onBuffering(percent: Int)
    fun onPlayingChanged(isPlaying: Boolean)

    fun onMediaItemChange()

    fun onMediaItemCleared()


    fun onIdle()

}