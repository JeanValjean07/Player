package com.suming.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

@OptIn(UnstableApi::class)
class ToolPlayerWrapper(player: ExoPlayer) : ForwardingPlayer(player) {

    override fun getAvailableCommands(): Player.Commands =
        super.getAvailableCommands()
            .buildUpon()
            .add(COMMAND_SEEK_TO_NEXT)
            .add(COMMAND_SEEK_TO_PREVIOUS)
            .add(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
            .build()

    //下一曲
    override fun seekToNext() {
        super.seekToNext()
        ToolEventBus.sendEvent("SessionController_Next")
    }
    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()
        ToolEventBus.sendEvent("SessionController_Next")
    }
    //上一曲
    override fun seekToPrevious() {
        super.seekToPrevious()
        ToolEventBus.sendEvent("SessionController_Previous")
    }
    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()
        ToolEventBus.sendEvent("SessionController_Previous")
    }
    //播放或暂停
    override fun play() {
        super.play()
        ToolEventBus.sendEvent("SessionController_Play")
    }
    override fun pause() {
        super.pause()
        ToolEventBus.sendEvent("SessionController_Pause")
    }

}