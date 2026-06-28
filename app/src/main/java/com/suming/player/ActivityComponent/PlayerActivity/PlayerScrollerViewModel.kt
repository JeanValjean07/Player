package com.suming.player.ActivityComponent.PlayerActivity

import androidx.databinding.ObservableArrayList
import androidx.lifecycle.ViewModel

@Suppress("unused")
class PlayerScrollerViewModel : ViewModel(){

    //可观察缩略图列表
    private val _frames = ObservableArrayList<scrollerItem>()
    val frames: ObservableArrayList<scrollerItem> = _frames

    //缩略图单项数据类
    data class scrollerItem(
        //当前缩略图类型,true为实图,false为占位图
        var currentThumbType: Boolean = false,
        //是否正在生成缩略图
        var thumbGeneratingRunning: Boolean = false,

    )


    //更新缩略图列表
    fun updateFrames(list: List<scrollerItem>) {
        _frames.clear()
        _frames.addAll(list)
    }



}