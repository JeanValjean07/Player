package com.suming.player.ActivityComponent.PlayerActivity

import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.suming.player.AddonTools.ToolEventBus

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

    }
    override fun seekToNextMediaItem() {
        super.seekToNextMediaItem()

    }
    //上一曲
    override fun seekToPrevious() {
        super.seekToPrevious()

    }
    override fun seekToPreviousMediaItem() {
        super.seekToPreviousMediaItem()

    }
    //播放或暂停
    override fun play() {
        super.play()

    }
    override fun pause() {
        super.pause()

    }

}