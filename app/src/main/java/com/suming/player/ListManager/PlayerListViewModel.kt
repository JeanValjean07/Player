package com.suming.player.ListManager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi

@UnstableApi
@Suppress("unused")
class PlayerListViewModel(application: Application) : AndroidViewModel(application) {

    //默认显式的页签
    var PREFS_AcquiescePage = -1
    //上一次显式的页签
    var state_LastPage = -1
    //当前播放列表
    var PREFS_CurrentPlayList = "video"







    override fun onCleared() {

    }
}