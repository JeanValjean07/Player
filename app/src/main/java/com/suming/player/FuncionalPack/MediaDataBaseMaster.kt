package com.suming.player.FuncionalPack

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.DataPack.DataBaseMediaItem.MediaItemRepo
import com.suming.player.DataPack.DataBaseMediaItem.MediaItemSetting
import com.suming.player.PlayerSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Suppress("unused")
object MediaDataBaseMaster {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaDataBaseMaster: $msg")
        }
    }




    //保存播放进度
    private val coroutine_save = CoroutineScope(SupervisorJob() + Dispatchers.IO)






    //读取单个媒体的所有设置并传回播放器 Long Tread
    private var mediaItemSettingLocal: MediaItemSetting? = null
    private var currentItemDataBaseID: String = ""
    private val coroutine_fetch = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @OptIn(UnstableApi::class)
    fun fetchMediaItemPack(itemID: String,context: Context){
        coroutine_fetch.launch {
            currentItemDataBaseID = itemID
            //读取该媒体的一行全部数据
            val mediaItemSetting = MediaItemRepo.get(context).getMediaItemPack(itemID)
            mediaItemSettingLocal = mediaItemSetting

            //发回播放器
            if (mediaItemSetting != null){
                PlayerSingleton.receiveParameters(mediaItemSetting)
            }

            //在本地进行检查,确认是否有需要改动的内容
            //1.是否要持续保存进度
            if(mediaItemSetting?.PREFS_SaveProgress == true){
                startSaveProgressHandler()
            }else{
                stopSaveProgressHandler()
            }
        }

    }

    //获取单项配置(目前仅开放保存播放进度这一项配置作为单项可调)
    //该媒体是否需要保存播放进度
    private var Para_saveProgress = false
    fun get_PREFS_saveProgress(itemID: String = "",context: Context): Boolean{
        if (currentItemDataBaseID != itemID){
            coroutine_fetch.launch {
                Para_saveProgress = MediaItemRepo.get(context).get_PREFS_saveLastPosition(itemID)
                //ID发生变更
                fetchMediaItemPack(itemID,context)
            }
        }

        return Para_saveProgress
    }
    fun set_PREFS_saveProgress(itemID: String = "", boolean: Boolean, context: Context){
        Para_saveProgress = boolean

        //保存到数据库
        coroutine_save.launch {
            MediaItemRepo.get(context).update_PREFS_saveLastPosition(itemID,boolean)
        }

        //开启保存进度循环
        if (boolean){
            startSaveProgressHandler()
        }else{
            stopSaveProgressHandler()
        }

    }
    //上次保存的播放进度
    private var Para_LastPosition = 0L
    fun get_State_LastPosition(itemID: String,context: Context): Long{
        if (currentItemDataBaseID != itemID){
            coroutine_fetch.launch {
                Para_LastPosition = MediaItemRepo.get(context).get_value_LastPosition(itemID)
                //ID发生变更
                fetchMediaItemPack(itemID,context)
            }
        }

        return Para_LastPosition
    }
    fun saveProgress(itemID: String, currentPosition:Long,duration: Long ,context: Context){
        coroutine_save.launch {
            //检查该媒体是否开启了保存进度选项
            val save = MediaItemRepo.get(context).get_PREFS_saveLastPosition(itemID)
            if (save) {
                //检查当前进度是否有效(大于0且小于总时长)
                if (currentPosition !in 0..duration) return@launch

                //保存进度
                MediaItemRepo.get(context).update_value_LastPosition(itemID,currentPosition)
            }
        }
    }
    //持续保存播放进度
    private var state_saveProgress_Running = false
    private val saveProgressHandler = Handler(Looper.getMainLooper())
    private var saveProgress = object : Runnable{
        @OptIn(UnstableApi::class)
        override fun run() {
            //从播放器拿当前进度和duration
            val currentPosition = PlayerSingleton.getState_currentPosition()
            val duration = PlayerInfoCenter.getMediaDuration()
            val itemDataBaseID = PlayerInfoCenter.getItemDataBaseID()
            val context = PlayerSingleton.getApplicationContext()

            if (duration <= 0) return
            if (itemDataBaseID == "" || itemDataBaseID != currentItemDataBaseID) return

            if (currentPosition in 0..duration){
                saveProgress(currentItemDataBaseID, currentPosition, duration,context)
            }


            saveProgressHandler.postDelayed(this, 20_000)
        }
    }
    private fun startSaveProgressHandler() {
        if (state_saveProgress_Running) return
        saveProgressHandler.post(saveProgress)
        state_saveProgress_Running = true
    }
    private fun stopSaveProgressHandler() {
        saveProgressHandler.removeCallbacks(saveProgress)
        state_saveProgress_Running = false
    }




}