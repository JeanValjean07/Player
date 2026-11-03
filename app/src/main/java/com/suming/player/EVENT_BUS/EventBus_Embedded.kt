package com.suming.player.EVENT_BUS

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus_Embedded {


    private val _events = MutableSharedFlow<AppEvent>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * 发射事件。
     * - 有活跃收集者：正常投递
     * - 无收集者（接收方未存活/未订阅）：直接丢弃，不抛异常
     */
    fun emit(event: AppEvent) {
        //tryEmit 在无收集者时返回 true 并丢弃事件
        //静默丢弃
        _events.tryEmit(event)
    }
}