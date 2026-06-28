package com.suming.player

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.PlayerInfoCenter

class ExternalInvokeManager : AppCompatActivity(){


    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBusiness()

        finish()

    }

    //主业务
    private fun mainBusiness(){
        //提取uri
        val (source,uri) = ExtractMediaUri(intent)
        val uriString = uri.toString()

        //区分来源
        when(source){
            //以新链接为目标
            1 -> {
                processOutSource(uriString, source)
            }
            //以正在播放项为目标
            2 -> {
                processPending()
            }
            else -> {
                consoleLog("来源不明")
                fail()
            }
        }

    }


    //从intent提取uri和source
    private fun ExtractMediaUri(intent: Intent): Pair<Int, Uri> {
        when (intent.action) {
            //系统面板：分享
            Intent.ACTION_SEND -> {
                val intentUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?: Uri.EMPTY

                return Pair(1,intentUri)
            }
            //系统面板：选择其他应用打开
            Intent.ACTION_VIEW -> {
                val intentUri = intent.data ?: Uri.EMPTY

                return Pair(1,intentUri)
            }
            //正常打开
            else -> {
                //来自pendingIntent
                if (intent.getStringExtra("IntentSource") == "FromPendingIntent"){
                    //来自pendingIntent时直接拉起播放页,不关注也不传入链接

                    return Pair(2, Uri.EMPTY)
                }
                //来自常规启动
                else{
                    //来自常规启动时,不需要关注链接是否存在,由播放页处理
                    val intentUri = IntentCompat.getParcelableExtra(intent, "uri", Uri::class.java)?: Uri.EMPTY

                    return Pair(3,intentUri)
                }
            }
        }
    }
    //以新链接为目标
    private fun processOutSource(targetUriString: String,source: Int){
        consoleLog("以新链接为目标 processOutSource")
        if (targetUriString != ""){
            //检查链接有效性和媒体类型
            val (success, mediaType) = MediaInfoRetriever.getUriStringMediaType(this,targetUriString)
            if (success){
                startPageByMediaType(targetUriString.toUri(), source,mediaType)
            }else{
                consoleLog("链接有效性检查失败")
                fail()
            }
        }else{
            consoleLog("新链接为空,无法播放")
            fail()
        }
    }
    //以正在播放项为目标
    @OptIn(UnstableApi::class)
    private fun processPending(){
        consoleLog("以正在播放项为目标 processPending")
        //检查正在播放的媒体
        val (ongoing , uri) = PlayerSingleton.getState_currentMediaItem_Uri()
        val uriString = uri.toString()
        if (ongoing){
            if (uriString != ""){
                startVideoPage(uri)
            }else{
                fail("页面启动失败")
            }
        }else{
            fail("当前未播放任何媒体")
        }
    }





    private fun fail(failMsg: String = "失败"){
        showCustomToast(failMsg)
        finishAndRemoveTask()
        return
    }



    //根据媒体类型启动
    @OptIn(UnstableApi::class)
    private fun startPageByMediaType(uri: Uri, source: Int, type: String){
        //根据类型启动页面
        //来自pendingIntent
        if (source == 2){
            when(type){
                "video" -> startVideoPage(uri)
                "music" -> startMusicPage(uri)
                else -> {
                    showCustomToast("失败",3)
                    finishAndRemoveTask()
                    return
                }

            }
        }
        //来自外部启动
        else if(source == 1){
            //先主动判断媒体类型
            val (success,mediaType) = getMediaInfo(this,uri)
            if (!success){
                showCustomToast("无法解码媒体信息,播放失败",3)
                finish()
                return
            }

            when(mediaType){
                "video" -> startVideoPage(uri)
                "music" -> startMusicPage(uri)
                else -> {
                    showCustomToast("无法验证媒体类型",3)
                    finish()
                }
            }
        }

    }
    //获取媒体信息
    private fun getMediaInfo(context: Context, uri: Uri): Pair<Boolean,String>{
        val retriever = MediaMetadataRetriever()
        //测试是否能正常读取
        try { retriever.setDataSource(context, uri) }
        catch (_: Exception) { return Pair(false,"") }
        //获取新的媒体信息
        val NEW_MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

        retriever.release()
        //处理值
        return if (NEW_MediaInfo_MediaType.contains("video")){
            Pair(true,"video")
        } else if(NEW_MediaInfo_MediaType.contains("audio")){
            Pair(true,"music")
        }else {
            Pair(false,"")
        }
    }
    //启动视频页面
    @OptIn(UnstableApi::class)
    private fun startVideoPage(uri: Uri) {
        val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)

        when(playPageType){
            0 -> startVideoOroPage(uri)
            1 -> startVideoNeoPage(uri)
        }

    }
    //启动视频页面neo
    @OptIn(UnstableApi::class)
    private fun startVideoNeoPage(uri: Uri) {
        //构建intent
        val intent = Intent(this, PlayerActivityNeo::class.java).apply {
            putExtra("uri", uri)
            action = "ACTION_NEW_INTENT"
        }.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)


        //启动
        startActivity(intent)
    }
    //启动视频页面oro
    @OptIn(UnstableApi::class)
    private fun startVideoOroPage(uri: Uri) {
        //构建intent
        val intent = Intent(this, PlayerActivityOro::class.java).apply { putExtra("uri", uri) }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

        //启动
        startActivity(intent)
    }
    //启动音乐页面
    private fun startMusicPage(uri: Uri) {

    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "ExternalInvokeManager: $msg")
        }
    }



}