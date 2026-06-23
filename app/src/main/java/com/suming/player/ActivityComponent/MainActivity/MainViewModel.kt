package com.suming.player.ActivityComponent.MainActivity

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.util.UnstableApi
import com.suming.player.SettingsRequestCenter

@UnstableApi
class MainViewModel(application: Application) : AndroidViewModel(application) {

    //状态值
    var state_current_tab: String = SettingsRequestCenter.tab_mark_null








}