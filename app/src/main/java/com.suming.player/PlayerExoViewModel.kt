package com.suming.player

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector


@UnstableApi
class PlayerExoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application

    val player: ExoPlayer
        get() = PlayerExoSingleton.getPlayer(app)

    val trackSelector: DefaultTrackSelector
        get() = PlayerExoSingleton.getTrackSelector(app)

    val rendererFactory: RenderersFactory
        get() = PlayerExoSingleton.getRendererFactory(app)


    fun setVideoUri(videoUri: Uri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
    }

    fun selectAudioOnly() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, true)
            .build()
    }

    fun recoveryAllTrack() {
        trackSelector.parameters = trackSelector
            .buildUponParameters()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .build()
    }



    override fun onCleared() {}
}