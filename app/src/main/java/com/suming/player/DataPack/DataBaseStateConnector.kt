package com.suming.player.DataPack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DataBaseStateConnector {



    //音乐加载状态
    private val _state_queryDisk_Music_state = MutableStateFlow(state_queryDisk_idle)
    val state_queryDisk_Music_state: StateFlow<Int> = _state_queryDisk_Music_state.asStateFlow()
    fun setState_queryDisk_Music_state(state: Int){
        _state_queryDisk_Music_state.value = state
    }

    //视频加载状态
    private val _state_queryDisk_Video_state = MutableStateFlow(state_queryDisk_idle)
    val state_queryDisk_Video_state: StateFlow<Int> = _state_queryDisk_Video_state.asStateFlow()
    fun setState_queryDisk_Video_state(state: Int){
        _state_queryDisk_Video_state.value = state
    }

    //通用状态(只要有一个在加载,就认为在加载中)
    private val _state_queryDisk = MutableStateFlow(state_queryDisk_idle)
    val state_queryDisk: StateFlow<Int> = _state_queryDisk.asStateFlow()
    fun setState_queryDisk(state: Int){
        _state_queryDisk.value = state
    }



    const val state_queryDisk_start = 1
    const val state_queryDisk_success = 2
    const val state_queryDisk_idle = 0



    const val state_queryDisk_Music_start = "state_queryDisk_Music_started"
    const val state_queryDisk_Video_start = "state_queryDisk_Video_started"
    const val state_queryDisk_Music_success = "state_queryDisk_Music_success"
    const val state_queryDisk_Video_success = "state_queryDisk_Video_success"


    const val state_queryDataBase_Music_start = "state_queryDataBase_Music_started"
    const val state_queryDataBase_Music_success = "state_queryDataBase_Music_success"
    const val state_queryDataBase_Video_start = "state_queryDataBase_Video_started"
    const val state_queryDataBase_Video_success = "state_queryDataBase_Video_success"


}