package com.suming.player

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


    //内部列表无下一个媒体时触发seekToNextMediaItem()，有下一个媒体时触发seekToNext()
    override fun seekToNext() {
        super.seekToNext()
        ToolEventBus.sendEvent("SessionController_Next")
    }

    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()
        ToolEventBus.sendEvent("SessionController_Next")
    }

    override fun seekToPrevious() {
        super.seekToPrevious()
        ToolEventBus.sendEvent("SessionController_Previous")
    }




}