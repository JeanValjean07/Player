package com.suming.player

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

data class Event(
    val key: String,
    val stringInfo: String? = null
)



object ToolEventBus {
    //基本通道
    private val eventSubject = PublishSubject.create<String>()
    val events: Observable<String> = eventSubject.hide()

    fun sendEvent(event: String) {
        eventSubject.onNext(event)
    }



    //复杂通道:可带一个额外字符串
    private val eventSubject_withExtraString = PublishSubject.create<Event>()
    val events_withExtraString: Observable<Event> = eventSubject_withExtraString.hide()

    fun sendEvent_withExtraString(event: Event) {
        eventSubject_withExtraString.onNext(event)
    }


}
