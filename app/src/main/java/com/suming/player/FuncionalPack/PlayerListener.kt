package com.suming.player.FuncionalPack

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.suming.player.PlayerSingleton

@Suppress("unused")
object PlayerListener {

    //应用引用
    private lateinit var context: Application
    fun setContext(context: Context){
        //检查是不是applicationContext
        if (context is Application) {
            //consoleLog("PlayerListener.setContext")
            this.context = context
        }else{
            consoleLog("PlayerListener.setContext error")
        }
    }
    fun getApplicationContext(): Context = context.applicationContext

    //音频管理器
    private var audioManager: AudioManager? = null
    private fun initAudioManager(context: Context){
        if (audioManager != null) return
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private fun MakeSureAudioManagerOnline(context: Context){
        if (audioManager == null){
            initAudioManager(context)
        }
    }

    //音频设备监听
    private val DeviceCallback = object : AudioDeviceCallback() {
        //有设备断开了
        @OptIn(UnstableApi::class)
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            val relevant = removedDevices.filter {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
            }
            //移除设备为耳机
            if (relevant.isNotEmpty()) {
                //consoleLog("PlayerListener.onAudioDevicesRemoved")

                state_HeadSetInserted = false
                //暂停播放
                PlayerSingleton.setState_forcePause()
            }
        }
        //有设备连接了
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            val relevant = addedDevices.filter {
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            if (relevant.isNotEmpty()) {
                //consoleLog("PlayerListener.onAudioDevicesAdded")

                state_HeadSetInserted = true
                //连接耳机时,若当前音量太高,限制一次
                setVolumeLimitWhenHeadSetPlug()
            }
        }
    }
    private fun startAudioDeviceCallback(context: Context){
        MakeSureAudioManagerOnline(context)
        //注册音频设备回调
        if (state_DeviceCallback_Registered) return
        state_DeviceCallback_Registered = true
        audioManager?.registerAudioDeviceCallback(DeviceCallback, null)
    }
    private fun stopAudioDeviceCallback(context: Context){
        MakeSureAudioManagerOnline(context)
        //注销音频设备回调
        if (!state_DeviceCallback_Registered) return
        state_DeviceCallback_Registered = false
        audioManager?.unregisterAudioDeviceCallback(DeviceCallback)
    }
    private var state_DeviceCallback_Registered = false
    private var state_HeadSetInserted = false
    //外部检查是否链接了耳机
    fun getState_isHeadsetPlugged(context: Context): Boolean {

        return state_HeadSetInserted
    }
    //插入耳机时,检查音量是否超过限制
    fun setVolumeLimitWhenHeadSetPlug(){
        if (audioManager == null) return

        val cacheAudioManager = audioManager
        if (cacheAudioManager != null){
            val maxVolume = cacheAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVolume = cacheAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (currentVolume >= (maxVolume*0.6).toInt()){
                cacheAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume*0.6).toInt(), 0)
            }
        }

        //cacheAudioManager = null 函数作用域会自己回收
    }




    //音频焦点监听(需要隔离focusRequest的初始化和开启操作)
    //注意:申请一次焦点,才能开始监听,无法在不申请焦点的情况下直接监听
    private var focusRequest: AudioFocusRequest? = null
    private fun initFocusRequest(context: Context){
        //仅初始化focusRequest,但不开启
        if(focusRequest != null) return
        //
        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                HandleFocusChange(focusChange)
            }
            .build()
    }
    private fun MakeSureFocusServiceOnline(context: Context){
        if (focusRequest == null) initFocusRequest(context)
    }
    var isFocus = false
    var state_needContinue_whenFocusGain = true     //获得焦点时是否继续
    var state_needPause_whenFocusLoss = true   //丢焦点时是否自动暂停

    @OptIn(UnstableApi::class)
    private fun HandleFocusChange(focusChange: Int){
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                //consoleLog("stopFocusChange - AUDIOFOCUS_LOSS")
                isFocus = false

                //暂停播放
                if (state_needPause_whenFocusLoss){
                    PlayerSingleton.pausePlay()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                //consoleLog("stopFocusChange - AUDIOFOCUS_LOSS_TRANSIENT")
                isFocus = false
                //暂停播放
                if (state_needPause_whenFocusLoss){
                    PlayerSingleton.pausePlay()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                //consoleLog("stopFocusChange - AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK")
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                //consoleLog("stopFocusChange - AUDIOFOCUS_GAIN")
                isFocus = true
                //强制暂停时不继续
                if (PlayerSingleton.getState_forcePause()) return
                //播放结束时不继续
                if (PlayerSingleton.GET_STE_playEnd()) return
                //检查是否需要继续播放
                if (state_needContinue_whenFocusGain){
                    PlayerSingleton.continuePlay(true)
                }
            }
        }
    }
    @OptIn(UnstableApi::class)
    //请求/释放音频焦点且启动/停止焦点监听
    fun requestAudioFocus(context: Context, force_request: Boolean){
        MakeSureAudioManagerOnline(context)
        MakeSureFocusServiceOnline(context)

        if (force_request){
            if (focusRequest != null) {
                audioManager?.requestAudioFocus(focusRequest!!)
            }
        }else{
            //检查播放器当前是否在播放
            val player = PlayerSingleton.getPlayer()
            if (player == null) return
            if (player.isPlaying) {
                audioManager?.requestAudioFocus(focusRequest!!)
            }

        }
    }
    fun giveupAudioFocus(context: Context){
        MakeSureAudioManagerOnline(context)
        //既丢弃了焦点,也停止了监听
        if (focusRequest == null) return
        audioManager?.abandonAudioFocusRequest(focusRequest!!)
        focusRequest = null
    }


    //开启/关闭所有可常驻的监听器
    fun startListener(focus: Boolean = false){
        if (state_listeners_Registered) return
        state_listeners_Registered = true

        //consoleLog("startListener")

        //开启音频设备监听
        startAudioDeviceCallback(context)
        //仅在播放时申请焦点
        if (focus) requestAudioFocus(context, force_request = false)

    }
    fun stopListener(){
        if (!state_listeners_Registered) return

        //关闭音频设备监听
        stopAudioDeviceCallback(context)
        //关闭音频焦点监听
        giveupAudioFocus(context)

        state_listeners_Registered = false
    }
    private var state_listeners_Registered = false


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerListener: $msg")
        }
    }


}