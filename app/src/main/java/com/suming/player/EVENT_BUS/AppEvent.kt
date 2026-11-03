package com.suming.player.EVENT_BUS

class AppEvent (

    //一个事件槽
    val event: String = "",

    //一个附加字符串槽位
    val eventSlotString: String = "",

    //一个附加长整数槽位
    val eventSlotLong: Long = 0L,

    //一个附加整数槽位
    val eventSlotInt: Int = 0,

    //一个附加浮点数槽位
    val eventSlotFloat: Float = 0.0f

)