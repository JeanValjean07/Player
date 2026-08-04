package com.suming.player.FuncionalPack

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ConnectCenter {


    //封面更新事件
    const val connector_event_cover_frame_update = "connector_event_cover_frame_update"
    private var coverFrameUpdateEvent_targetFilePath: String = ""
    private var coverFrameUpdateEvent_targetMediaId: Long = 0
    fun setCoverFrameUpdateEvent_targetFileInfo(filePath: String, mediaId: Long){
        coverFrameUpdateEvent_targetFilePath = filePath
        coverFrameUpdateEvent_targetMediaId = mediaId
    }
    fun getCoverFrameUpdateEvent_targetFileInfo(): Pair<String, Long>{
        val cache = Pair(coverFrameUpdateEvent_targetFilePath, coverFrameUpdateEvent_targetMediaId)

        coverFrameUpdateEvent_targetFilePath = ""
        coverFrameUpdateEvent_targetMediaId = 0

        return cache
    }




    //杂项连接器
    private val _state_connector = MutableStateFlow("")
    val state_connector: StateFlow<String> = _state_connector.asStateFlow()
    fun setState_connector(state: String){
        _state_connector.value = state + System.currentTimeMillis()
    }
    fun clear_connector(){
        _state_connector.value = ""
    }























}