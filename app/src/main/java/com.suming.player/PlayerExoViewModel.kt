package com.suming.player

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.media3.common.C.WAKE_MODE_NETWORK
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class PlayerExoViewModel(application: Application): AndroidViewModel(application) {

    private val trackSelector = DefaultTrackSelector(application)
    val player = ExoPlayer.Builder(application)
    .setSeekParameters(SeekParameters.CLOSEST_SYNC)
    .setWakeMode(WAKE_MODE_NETWORK)
    .setTrackSelector(trackSelector)
    .build()
    .apply {
        prepare()
        playWhenReady = true
    }


    fun setVideoUri(videoUri: Uri){
        player.setMediaItem(MediaItem.fromUri(videoUri))
    }


    fun VM_playerSelectSoundTrack() {
        val trackSelector = DefaultTrackSelector(application)
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setMaxVideoSize(0, 0)
            .build()
    }
    fun VM_playerRecoveryAllTrack() {
        val trackSelector = DefaultTrackSelector(application)
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .build()
    }





    override fun onCleared() {
        player.release()
    }








}