package com.suming.player.FuncionalPack

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.DataPack.DataBaseMediaSingleSetting.MediaItemRepo
import com.suming.player.DataPack.DataBaseMediaSingleSetting.MediaItemDataClass
import com.suming.player.PlayerSingleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("unused")
object MediaDataBaseMaster {
    //context
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {

            this.context = context
        }
    }
    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MediaDataBaseMaster: $msg")
        }
    }
    //空字段
    const val Undefined = ""




    //保存播放进度
    private val coroutine_save = CoroutineScope(SupervisorJob() + Dispatchers.IO)






    //读取单个媒体的所有设置并传回播放器
    private var mediaItemSettingLocal: MediaItemDataClass? = null

    //协程
    private val coroutine_fetch = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    //取出单个媒体的设置包
    @OptIn(UnstableApi::class)
    fun fetchMediaItemPack(uniqueID_URI_S_FP: String,context: Context) {
        coroutine_fetch.launch {
            //读取该媒体的一行全部数据
            val mediaItemSetting = MediaItemRepo.get(context).getMediaItemPack(uniqueID_URI_S_FP)
            mediaItemSettingLocal = mediaItemSetting

            //发回播放器
            if (mediaItemSetting != null){
                withContext(Dispatchers.Main) {
                    PlayerSingleton.receiveParameters(mediaItemSetting)
                }
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
    suspend fun get_PREFS_saveProgress(uniqueID_URI_S_FP: String,context: Context): Boolean {
        if (uniqueID_URI_S_FP == Undefined) return false

        //检查数据库中是否有键值为uniqueID_URI_S_FP的项,没有时直接返回false
        val exist = MediaItemRepo.get(context).checkExist(uniqueID_URI_S_FP)
        if (exist){
            //存在时,拿到值
            Para_saveProgress = MediaItemRepo.get(context).get_PREFS_saveLastPosition(uniqueID_URI_S_FP)

            return Para_saveProgress

        }else{
            Para_saveProgress = false

            return false
        }
    }
    fun set_PREFS_saveProgress(uniqueID_URI_S_FP: String = Undefined, boolean: Boolean, context: Context) {
        Para_saveProgress = boolean

        //保存到数据库
        coroutine_save.launch {
            //先检查数据库中有没有该项,没有时新建
            if (!MediaItemRepo.get(context).checkExist(uniqueID_URI_S_FP)){
                MediaItemRepo.get(context).createMediaItem(uniqueID_URI_S_FP)
            }

            //写入设置
            MediaItemRepo.get(context).update_PREFS_saveLastPosition(uniqueID_URI_S_FP,boolean)
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
    fun getLastProgress(uniqueID_URI_S_FP: String, context: Context): Long {
        coroutine_fetch.launch {
            Para_LastPosition = MediaItemRepo.get(context).get_value_LastPosition(uniqueID_URI_S_FP)
            //ID发生变更
            fetchMediaItemPack(uniqueID_URI_S_FP,context)
        }

        return Para_LastPosition
    }
    fun saveProgress(uniqueID_URI_S_FP: String, currentPosition_o:Long,duration: Long ,context: Context) {
        coroutine_save.launch {
            //检查该媒体是否开启了保存进度选项
            val save = MediaItemRepo.get(context).get_PREFS_saveLastPosition(uniqueID_URI_S_FP)
            //consoleLog("收到保存请求:是否保存:$save")
            if (save) {
                //检查当前进度是否有效(大于0且小于总时长)
                if (currentPosition_o !in 0..duration) return@launch
                val currentPosition = if (currentPosition_o >= duration - 10000L) {
                    0
                }else{
                    currentPosition_o
                }
                //consoleLog("保存内容:uniqueID_URI_S_FP:$uniqueID_URI_S_FP,currentPosition_o:$currentPosition_o")

                //保存进度
                MediaItemRepo.get(context).update_value_LastPosition(uniqueID_URI_S_FP,currentPosition)
            }
        }
    }

    //持续保存播放进度
    private var state_saveProgress_Running = false
    private val saveProgressHandler = Handler(Looper.getMainLooper())
    private var saveProgress = object : Runnable {
        @OptIn(UnstableApi::class)
        override fun run() {
            coroutine_save.launch {
                delay(5000L)

                withContext(Dispatchers.Main){
                    //从播放器拿当前进度和duration
                    val currentPosition = PlayerSingleton.get_ongoing_current_position()
                    val duration = PlayerInfoCenter.GET_Media_Duration()
                    val uniqueID_URI_S_FP = PlayerInfoCenter.GET_Media_URI_S_FP()

                    //
                    if (duration <= 0) return@withContext
                    if (currentPosition < 0) return@withContext

                    if (currentPosition in 0..duration){
                        saveProgress(uniqueID_URI_S_FP, currentPosition, duration,context)
                    }

                }

            }


            saveProgressHandler.postDelayed(this, 15_000)
        }
    }
    private fun startSaveProgressHandler() {
        if (state_saveProgress_Running) return
        state_saveProgress_Running = true

        saveProgressHandler.post(saveProgress)

    }
    private fun stopSaveProgressHandler() {
        if (!state_saveProgress_Running) return
        state_saveProgress_Running = false

        saveProgressHandler.removeCallbacks(saveProgress)

    }




}