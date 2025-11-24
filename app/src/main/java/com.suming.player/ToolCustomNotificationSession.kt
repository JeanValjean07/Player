package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList

@OptIn(UnstableApi::class)
class ToolCustomNotificationSession(context: Context): DefaultMediaNotificationProvider(context) {

    @SuppressLint("RestrictedApi")
    @OptIn(UnstableApi::class)
    override fun getMediaButtons(session: MediaSession, playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showPauseButton: Boolean): ImmutableList<CommandButton> {

        val list = ImmutableList.builder<CommandButton>()

        //上一曲
        if (playerCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS)) {
            list.add(
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .setCustomIconResId(R.drawable.session_controller_previous)
                    .setDisplayName("Previous")
                    .build()
            )
        }

        //播放/暂停
        val playPauseCommand = Player.COMMAND_PLAY_PAUSE

        list.add(
            CommandButton.Builder()
                .setPlayerCommand(playPauseCommand)
                .setCustomIconResId(if (showPauseButton) R.drawable.session_controller_pause else R.drawable.session_controller_play)
                .setDisplayName(if (showPauseButton) "Pause" else "Play")
                .build()
        )

        //下一曲
        if (playerCommands.contains(Player.COMMAND_SEEK_TO_NEXT)) {
            list.add(
                CommandButton.Builder()
                    .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                    .setCustomIconResId(R.drawable.session_controller_next)
                    .setDisplayName("Next")
                    .build()
            )
        }

        return list.build()
    }
}

