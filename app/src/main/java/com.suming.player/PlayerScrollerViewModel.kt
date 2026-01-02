package com.suming.player

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.databinding.ObservableArrayList
import androidx.lifecycle.ViewModel
import java.io.File

@Suppress("unused")
class PlayerScrollerViewModel():ViewModel(){

    //private val _thumbItems = MutableList(500) { ThumbScrollerItem() }

    private val _thumbItems = ObservableArrayList<ThumbScrollerItem>()

    val thumbItems: ObservableArrayList<ThumbScrollerItem> = _thumbItems


    data class ThumbScrollerItem(

        var currentThumbType: Boolean = false,         //当前缩略图类型,true为实图,false为占位图

        var thumbGeneratingRunning: Boolean = false,   //是否正在生成缩略图

    )

    var last_MediaInfo_FileName = ""

    fun updateThumbs(list: List<ThumbScrollerItem>) {
        _thumbItems.clear()
        _thumbItems.addAll(list)
    }



}