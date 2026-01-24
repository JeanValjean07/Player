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
import androidx.media3.common.util.UnstableApi

class ExternalInvokeManager : AppCompatActivity(){




    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        //启动来源标记 < 1 = ACTION_SEND/ACTION_VIEW 丨 2 = pending 丨 3 = 常规启动 >
        val (source,uri) = ExtractMediaUri(intent)

        //根据媒体类型启动页面
        startPageByMediaType(uri,source)




        finish()

    }


    //从intent提取uri信息
    private fun ExtractMediaUri(intent: Intent): Pair<Int,Uri> {
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

                    return Pair(2,Uri.EMPTY)

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

    //根据媒体类型启动
    @OptIn(UnstableApi::class)
    private fun startPageByMediaType(uri: Uri,source: Int){
        //从播放器获取类型
        val mediaType = PlayerSingleton.getMediaInfoType()




        //根据类型启动页面
        if (source == 2){
            when(mediaType){
                "video" -> startVideoPage(uri,source)
                "music" -> startMusicPage(uri,source)
                else -> {
                    showCustomToast("无法验证媒体类型",3)
                    finish()
                }
            }
        }else if(source == 1){
            //先主动判断媒体类型
            val (success,mediaType) = getMediaInfo(this,uri)
            if (!success){
                showCustomToast("无法解码媒体信息,播放失败",3)
                finish()
                return
            }


            when(mediaType){
                "video" -> startVideoPage(uri,source)
                "music" -> startMusicPage(uri,source)
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
    private fun startVideoPage(uri: Uri,source: Int) {
        val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)

        when(playPageType){
            0 -> startVideoOroPage(uri,source)
            1 -> startVideoNeoPage(uri,source)
        }

        //构建intent
        val intent = Intent(this, PlayerActivityNeo::class.java).apply { putExtra("uri", uri) }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra("IntentSource", source)

        //启动
        startActivity(intent)
    }
    //启动视频页面neo
    @OptIn(UnstableApi::class)
    private fun startVideoNeoPage(uri: Uri,source: Int) {
        //构建intent
        val intent = Intent(this, PlayerActivityNeo::class.java).apply {
            putExtra("uri", uri)
            action = "ACTION_NEW_INTENT"
        }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            .putExtra("IntentSource", source)


        Log.d("SuMing","invoke startVideoNeoPage: $uri")
        //启动
        startActivity(intent)
    }
    //启动视频页面oro
    @OptIn(UnstableApi::class)
    private fun startVideoOroPage(uri: Uri,source: Int) {
        //构建intent
        val intent = Intent(this, PlayerActivityOro::class.java).apply { putExtra("uri", uri) }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            .putExtra("IntentSource", source)

        //启动
        startActivity(intent)
    }
    //启动音乐页面
    private fun startMusicPage(uri: Uri,source: Int) {

    }



}