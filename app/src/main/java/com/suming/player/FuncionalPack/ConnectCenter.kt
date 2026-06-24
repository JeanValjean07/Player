package com.suming.player.FuncionalPack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConnectCenter {


    //封面更新事件
    const val connector_event_cover_frame_update = "connector_event_cover_frame_update"
    private var coverFrameUpdateEvent_targetUriNumOnly: Long = 0
    fun setCoverFrameUpdateEvent_targetUriNumOnly(uriNumOnly: Long){
        coverFrameUpdateEvent_targetUriNumOnly = uriNumOnly
    }
    fun getCoverFrameUpdateEvent_targetUriNumOnly(): Long{
        val cache = coverFrameUpdateEvent_targetUriNumOnly

        coverFrameUpdateEvent_targetUriNumOnly = 0

        return cache
    }

    //杂项连接器
    private val _state_connector = MutableStateFlow("")
    val state_connector: StateFlow<String> = _state_connector.asStateFlow()
    fun setState_connector(state: String){
        _state_connector.value = state + System.currentTimeMillis()
    }























}