package com.suming.player

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

object ToolEventBus {

    private val eventSubject = PublishSubject.create<String>()

    //暴露Observable
    val events: Observable<String> = eventSubject.hide()


    fun sendEvent(event: String) {
        eventSubject.onNext(event)
    }
}
