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
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.MediaUriManager
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PrivacyPermissionHelper

@Suppress("NewApi")
class EntranceActivity : AppCompatActivity(){

    //空字段
    private val Undefined = ""



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

        //根据来源处理
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
        consoleLog("以新链接为目标 processOutSource ")
        if (targetUriString != Undefined){
            //分支-新链接不为空
            consoleLog("processOutSource 新链接不为空-targetUriString: $targetUriString source:$source")

            //检查链接有效性和媒体类型(这个居然不需要任何权限就能查)
            val (success, mediaType) = MediaInfoRetriever.getUriValidAndMediaType(this,targetUriString)
            consoleLog("processOutSource-链接有效性检查结果: success:$success, mediaType:$mediaType")
            if (!success){ fail("播放失败(媒体无效)") ; return }

            //检查链接的权限级别(是否属于被降级的底权限链接)
            val uriTypeMode = MediaUriManager.detectMediaUriTypeMode(targetUriString.toUri())
            when (uriTypeMode){
                //完整权限链接(content://media/external/audio|video|image 在audio|video|image表中)
                MediaUriManager.uriType_full_permission -> {
                    consoleLog("processOutSource -原始链接是完整权限链接: $targetUriString")

                    //启动播放页
                    startPage_selfDetectMediaType(targetUriString.toUri(), source,mediaType)

                }
                //低权限链接(content://media/external/file 在file表中)
                MediaUriManager.uriType_low_permission -> {
                    consoleLog("processOutSource -原始链接是低权限链接: $targetUriString")

                    //检查权限
                    val privacyPermissionHelper = PrivacyPermissionHelper()
                    val isAllFilesAccessGranted = privacyPermissionHelper.isAllFilesAccessGranted()
                    if (!isAllFilesAccessGranted){ fail("播放失败(请授权所有文件访问权限)") ; return }

                    //将低权限链接转换为标准链接(这一步需要所有文件访问权限才行)
                    val standardUri = MediaUriManager.convertFileUriToMediaUri(this, targetUriString.toUri())
                    consoleLog("processOutSource-转换后的标准链接: $standardUri")
                    if (standardUri == Uri.EMPTY){ fail("播放失败(标准链接转码失败)") ; return }

                    //启动播放页
                    startPage_selfDetectMediaType(standardUri, source,mediaType)

                }
                //链接不知所云(缺乏正常链接的结构)
                MediaUriManager.uriType_null -> {
                    consoleLog("processOutSource -uriTypeMode -链接不知所云")

                    fail("播放失败(链接不知所云)")
                }
            }

        }else{
            //分支-新链接为空
            consoleLog("新链接为空,无法播放")

            fail("播放失败(未传入媒体基本信息)")
        }
    }
    //以正在播放项为目标
    @OptIn(UnstableApi::class)
    private fun processPending(){
        consoleLog("以正在播放项为目标 processPending")

        //获取正在播放的媒体链接
        val (ongoing , uri) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
        val uriString = uri.toString()
        //获取正在播放的媒体类型
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        //根据媒体类型启动页面
        when(mediaType){
            MediaType.Video -> {
                if (ongoing){
                    if (uriString != Undefined){

                        //打开页面
                        startVideoPage_selfDetectStyle(uri)

                    }else{
                        //按理说不会走到这里,因为不可能正在播放空链接,如果出现非预期情况,关闭播放器作为保底
                        //关闭播放器
                        PlayerSingleton.stopPlayEngine()

                        fail("打开页面失败(未知错误)")
                    }
                }else{
                    fail("打开页面失败(当前未播放任何媒体)")
                }
            }
            MediaType.Audio -> {

                fail("打开页面失败(暂不支持音乐页面)")
            }
            else -> {
                //按理说不会出现不支持的媒体类型,因为播放前就有一道检查,如果出现非预期情况,关闭播放器作为保底
                //关闭播放器
                PlayerSingleton.stopPlayEngine()

                fail("打开页面失败(不支持的媒体类型)")
            }
        }

    }




    //启动失败提示和自动退出
    private fun fail(failMsg: String = Undefined){
        if (failMsg != Undefined){
            showCustomToast(failMsg)
        }
        //关闭当前活动
        finishAndRemoveTask()

        return
    }



    //根据媒体类型启动播放页(自己判断媒体类型或者主动传入媒体类型)
    @OptIn(UnstableApi::class)
    private fun startPage_selfDetectMediaType(uri: Uri, source: Int, mediaType: String){
        //根据发起来源选择启动页面
        when(source){
            //来自外部启动(以新传入的媒体为主)
            1 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
            //来自pendingIntent(以正在播放的媒体为主)
            2 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
        }

    }
    private fun startPage_selfDetectMediaType(uri: Uri, source: Int){
        //先主动判断媒体类型
        val (success,mediaType) = RTV_MediaType(this,uri)
        if (!success){ fail("播放失败(无法解码和判断媒体类型)") ; return }

        //根据发起来源选择启动页面
        when(source){
            //来自外部启动(以新传入的媒体为主)
            1 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
            //来自pendingIntent(以正在播放的媒体为主)
            2 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
        }

    }



    //启动视频页面(自己判断页面样式)
    @OptIn(UnstableApi::class)
    private fun startVideoPage_selfDetectStyle(uri: Uri) {
        //读取页面样式
        val playPageType = SettingsRequestCenter.get_PREFS_PlayPageType(this)
        //根据页面样式启动页面
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
        }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

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
    @OptIn(UnstableApi::class)
    private fun startMusicPage(uri: Uri) {
        //越权设置音频
        PlayerSingleton.setMediaItem(uri,true)

        //构建intent
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        //启动
        startActivity(intent)
    }




    //解码媒体类型(顺带查询能否解码)
    private fun RTV_MediaType(context: Context, uri: Uri): Pair<Boolean,String>{
        val retriever = MediaMetadataRetriever()
        //测试是否能正常解码
        try {
            retriever.setDataSource(context, uri)
        }catch(e: Exception){
            consoleLog("getMediaInfo-媒体无法解码:${e.message}")

            return Pair(false,"")
        }
        //获取新的媒体信息
        val NEW_MediaInfo_MediaType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""

        retriever.release()
        //处理值
        return if (NEW_MediaInfo_MediaType.contains("video")){
            Pair(true,MediaType.Video)
        } else if(NEW_MediaInfo_MediaType.contains("audio")){
            Pair(true,MediaType.Audio)
        }else {
            consoleLog("getMediaInfo-媒体类型未知不支持")
            Pair(false,"")
        }
    }


    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "EntranceActivity: $msg")
        }
    }


}