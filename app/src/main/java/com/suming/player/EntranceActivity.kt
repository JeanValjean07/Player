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
import com.suming.player.DataPack.DataClassForPlay.MediaItemForPlay
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.IntentRepo
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.MediaUriManager
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PrivacyPermissionHelper
import java.io.File

@Suppress("NewApi")
class EntranceActivity : AppCompatActivity(){
    companion object {
        private const val REQUEST_CODE_OPEN_DIRECTORY = 1001
    }

    //空字段
    private val Undefined = ""
    //ctx
    private val context = this@EntranceActivity



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBusiness()


        finish()

    }



    //主业务
    private fun mainBusiness(){
        //提取uri
        val (URI,SOURCE_CODE) = detectOriginalInfo_fromIntent(intent)
        consoleLog(" mainBusiness() -URI = $URI, -SOURCE_CODE = $SOURCE_CODE")
        val URI_String = URI.toString()

        //根据 SOURCE_CODE 处理
        when(SOURCE_CODE){
            //以新链接为目标
            1 -> {
                processOutSource(URI_String, SOURCE_CODE)
            }
            //以正在播放项为目标
            3 -> {
                processPending()
            }
            //未知来源
            else -> {
                consoleLog(" mainBusiness() -SOURCE_CODE 不合法")
                fail("未知错误")
            }
        }

    }

    //从Intent提取 URI 和 SOURCE
    private fun detectOriginalInfo_fromIntent(intent: Intent): Pair<Uri, Int> {
        when (intent.action) {
            //系统面板：分享
            Intent.ACTION_SEND -> {
                val intentUri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?: Uri.EMPTY

                return Pair(intentUri,1)
            }
            //系统面板：选择其他应用打开
            Intent.ACTION_VIEW -> {
                val intentUri = intent.data ?: Uri.EMPTY

                return Pair(intentUri,1)
            }
            //正常打开
            else -> {
                //来自pendingIntent
                if (intent.getStringExtra(IntentRepo.SOURCE) == "FromPendingIntent"){
                    //来自pendingIntent时直接拉起播放页,不关注也不传入链接

                    return Pair(Uri.EMPTY,2)
                }
                //来自常规启动
                else{
                    //来自常规启动时,不需要关注链接是否存在,由播放页处理
                    val intentUri = IntentCompat.getParcelableExtra(intent, IntentRepo.URI, Uri::class.java)?: Uri.EMPTY

                    return Pair(intentUri,3)
                }
            }
        }
    }

    //以新链接为目标(注意:以下传入链接类型可被ExoPlayer直接播放:1. MediaStore详细表链接 2.文件路径。警告：FileProvider链接不一定能直接播放,MediaStore文件表链接也不行)
    @OptIn(UnstableApi::class)
    private fun processOutSource(URI_String: String,SOURCE: Int){
        consoleLog("以新链接为目标 processOutSource ")
        if (URI_String != Undefined){
            //分支-新链接不为空
            //consoleLog("processOutSource 新链接不为空-原始链接: $URI_String 来源标记:$SOURCE")
            //典型值
            /*
            //MediaStore详细表链接(content://media/external/video/media/2599,content://media/external/audio/media/2537)
            //MediaStore文件表链接(content://media/external/file/2622) <- 一般是非公有文件夹和.nomedia文件夹出现这种链接
            //FileProvider链接:(不一定包含fileprovider字段,但一定包含storage/emulated/0字段)
            //content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4
            //content://com.coloros.filemanager/root/storage/emulated/0/Pictures/%E9%9F%B3%E4%B9%90%E8%A7%86%E9%A2%91/%E4%B8%8B%E5%B1%B1_DJ%E7%89%88.mp4
            //content://114514/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4
            //特殊链接
            //华为相册私有：/storage/emulated/0/Pictures/音乐视频/天空.mp4?bgcolor=-920587

             */

            val uriTypeMode = MediaUriManager.detectMediaUriTypeMode(URI_String.toUri())
            when (uriTypeMode){
                //MediaStore详细表链接(示例 content://media/external/video/media/2599,content://media/external/audio/media/2537)
                MediaUriManager.uriType_media_store_detail -> {
                    //consoleLog("processOutSource -原始链接是MediaStore详细表链接: $URI_String")

                    //MediaStore详细表链接可以直接尝试解码
                    executeByUriType_canRetriever(uriTypeMode,URI_String,SOURCE)

                }
                //MediaStore文件表链接(示例 content://media/external/file/2622)
                MediaUriManager.uriType_media_store_file -> {
                    consoleLog("processOutSource -原始链接是MediaStore文件表链接: $URI_String")

                    val privacyPermissionHelper = PrivacyPermissionHelper()
                    val isAllFilesAccessGranted = privacyPermissionHelper.isAllFilesAccessGranted()
                    if (isAllFilesAccessGranted){

                        //MediaStore文件表可以直接尝试解码,但无法播放
                        executeByUriType_canRetriever(uriTypeMode,URI_String,SOURCE)

                    }else{
                        fail("播放失败(请授权所有文件访问权限)")
                        return
                    }

                }
                //FileProvider链接(示例 content://fileprovider/filemanager/fileprovider/filemanager, content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4)
                MediaUriManager.uriType_file_provider -> {
                    consoleLog("processOutSource -原始链接是FileProvider链接: $URI_String")
                    //尝试直接取出文件路径

                    //FileProvider链接可以直接尝试解码
                    executeByUriType_canRetriever(uriTypeMode,URI_String,SOURCE)

                }
                //链接无法解析
                MediaUriManager.uriType_null -> {
                    consoleLog("processOutSource -uriTypeMode -无法解析链接: $URI_String")

                    fail("播放失败(无法解析链接:$URI_String)")
                }
                //特殊链接
                MediaUriManager.uriType_special -> {
                    consoleLog("processOutSource -uriTypeMode -特殊链接: $URI_String")

                    //这个无法直接解码,先转换成可以解码的链接
                    //对特殊链接进行处理(转换为FileProvider链接)
                    val processedUri = MediaUriManager.processSpecialUri(URI_String)
                    consoleLog("processOutSource -uriTypeMode -特殊链接处理结果: $processedUri")
                    if (processedUri == Uri.EMPTY){ fail("播放失败(特殊链接处理失败)") ; return }

                    //将FileProvider链接转换为标准链接
                    val URI_UP = MediaUriManager.convert_FileManagerFileURI_to_MediaStoreMediaURI(this, URI_String.toUri()).first
                    consoleLog("processOutSource-转换后的标准链接: URI_UP：$URI_UP")
                    if (URI_UP == Uri.EMPTY){
                        fail("播放失败(标准链接转换失败:$URI_UP)")
                    }else{
                        //成功转换出详细表标准链接(格式:content://media/external/video/media/2624)

                        //启动播放页
                        executeByUriType_canRetriever(MediaUriManager.uriType_file_provider,URI_UP.toString(),SOURCE)
                    }




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
        //consoleLog("以正在播放项为目标 processPending")

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
                        startVideoPage_selfDetectStyle(uri,Undefined,)

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


    //根据uriTypeMode处理(可直接尝试解码的类型)
    private fun executeByUriType_canRetriever(uriTypeMode: String,URI_String: String,SOURCE: Int){
        //尝试直接解码
        val (result,MediaItemForPlay,_) = MediaInfoRetriever.retrieveMediaInfo(context,URI_String)
        //失败检查+成功时写入PlayerInfoCenter
        when(result){
            ActivityResultConnector.retriever_type_not_support -> {
                fail("不支持的媒体格式")
                return
            }
            ActivityResultConnector.retriever_error -> {
                fail("初期解码进程失败")
                return
            }
            ActivityResultConnector.retriever_complete -> {
                //存到PlayerInfoCenter
                PlayerInfoCenter.SET_MediaItemForPlay_Pack(MediaItemForPlay)
            }
        }
        val mediaType = MediaItemForPlay.media_SPECIFIC_MediaType
        if (mediaType == MediaType.Undefined){
            fail("不支持的媒体格式")
            return
        }

        //区分uriTypeMode
        when (uriTypeMode){
            //MediaStore详细表链接(示例 content://media/external/video/media/2599,content://media/external/audio/media/2537)
            MediaUriManager.uriType_media_store_detail -> {
                //consoleLog("processOutSource -原始链接是MediaStore详细表链接: $URI_String")

                //启动播放页
                startPage_selfDetectMediaType(URI_String.toUri(),Undefined,SOURCE,mediaType)

            }
            //MediaStore文件表链接(示例 content://media/external/file/2622)
            MediaUriManager.uriType_media_store_file -> {
                consoleLog("processOutSource -原始链接是MediaStore文件表链接: $URI_String")

                val privacyPermissionHelper = PrivacyPermissionHelper()
                val isAllFilesAccessGranted = privacyPermissionHelper.isAllFilesAccessGranted()
                if (isAllFilesAccessGranted){
                    //启动播放(直接用File URI也可播放,无需再获取详细URI,而且根本也获取不到)
                    startPage_selfDetectMediaType(URI_String.toUri(),Undefined,SOURCE, mediaType)

                }else{
                    fail("播放失败(请授权所有文件访问权限)")
                    return
                }



                //尝试获取文件路径
                val file_path = MediaItemForPlay.file_path
                if (file_path == Undefined){
                    //尝试其他法子

                    //启动播放(直接用File URI也可播放,无需再获取详细URI,而且根本也获取不到)
                    startPage_selfDetectMediaType(URI_String.toUri(),Undefined,SOURCE, mediaType)

                }else{
                    consoleLog("processOutSource -文件路径 启动播放: file_path $file_path")
                    //启动播放(直接用File URI也可播放,无需再获取详细URI,而且根本也获取不到)
                    startPage_selfDetectMediaType(file_path.toUri(),Undefined,SOURCE, mediaType)

                }


            }
            //FileProvider链接(示例 content://fileprovider/filemanager/fileprovider/filemanager, content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4)
            MediaUriManager.uriType_file_provider -> {
                consoleLog("processOutSource -原始链接是FileProvider链接: $URI_String")

                //尝试获取文件路径
                val file_path = MediaItemForPlay.file_path
                if (file_path == Undefined){ fail("播放失败(无法获取文件路径)") ; return }
                //尝试查表获取标准链接
                val URI_Standard = MediaUriManager.detect_FilePath(file_path,context)
                val URI = if (URI_Standard == Undefined){
                    URI_String.toUri()
                }else{
                    URI_Standard.toUri()
                }
                consoleLog("processOutSource-转换后的标准链接: URI_Standard:$URI_Standard URI: $URI")

                //是否能成功转换出标准链接
                if (URI_Standard == Undefined){
                    //无法转换出标准链接,可能是文件夹非公有,或被.nomedia标记
                    consoleLog("无法转换出标准链接: $URI_String")

                    //原则上,运行到这里以及说明解码是成功了的,直接尝试播放
                    /*
                    //进行一次模糊判断:看看这个文件到底存不存在
                    //使用模糊判断 -检查获取到的文件路径
                    consoleLog("processOutSource -uriTypeMode -模糊判断 -获取到的文件路径: $file_path")
                    val file = File(file_path)
                    if (!file.exists()){ fail("播放失败(文件不存在)") ; return }

                     */

                    //播放
                    //startPage_selfDetectMediaType(URI,SOURCE,mediaType)
                    consoleLog("processOutSource -uriTypeMode -播放文件路径:file_path.toUri()= ${file_path.toUri()}")
                    startPage_selfDetectMediaType(file_path.toUri(),file_path,SOURCE,mediaType)


                }else{
                    //成功转换出详细表标准链接(格式:content://media/external/video/media/2624)
                    consoleLog("成功转换出详细表标准链接: $URI_Standard")

                    //启动播放页
                    startPage_selfDetectMediaType(URI,file_path,SOURCE,mediaType)
                }

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
    private fun startPage_selfDetectMediaType(uri: Uri,file_path: String, source: Int, mediaType: String){
        consoleLog("startPage_selfDetectMediaType -uri: $uri -source: $source -mediaType: $mediaType")

        //根据发起来源选择启动页面
        when(source){
            //来自外部启动(以新传入的媒体为主)
            1 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri,file_path)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
            //来自pendingIntent(以正在播放的媒体为主)
            2 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri,file_path)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
        }

    }
    private fun startPage_selfDetectMediaType(uri: Uri,file_path: String, source: Int){
        //先主动判断媒体类型
        val (success,mediaType) = RTV_MediaType(this,uri)
        if (!success){ fail("播放失败(无法解码和判断媒体类型)") ; return }

        //根据发起来源选择启动页面
        when(source){
            //来自外部启动(以新传入的媒体为主)
            1 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri,file_path)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
            //来自pendingIntent(以正在播放的媒体为主)
            2 -> {
                when(mediaType){
                    MediaType.Video -> startVideoPage_selfDetectStyle(uri,file_path)
                    MediaType.Audio -> startMusicPage(uri)
                    else -> fail("播放失败(不支持的媒体类型)")
                }
            }
        }

    }



    //启动视频页面(自己判断页面样式)
    @OptIn(UnstableApi::class)
    private fun startVideoPage_selfDetectStyle(uri: Uri,file_path: String) {
        //读取页面样式
        val playPageType = SettingsRequestCenter.GET_PRF_PlayPageType(this)
        //根据页面样式启动页面
        when{
            (playPageType == SettingsRequestCenter.PlayPageType_Oro || playPageType == SettingsRequestCenter.PlayPageType_Neo) -> startVideoNeoPage(uri,file_path)
            playPageType == SettingsRequestCenter.PlayPageType_Test -> {

            }
        }

    }
    //启动视频页面neo
    @OptIn(UnstableApi::class)
    private fun startVideoNeoPage(uri: Uri, file_path: String) {

        //构建intent
        val intent = Intent(this, PlayerActivityNeo::class.java).apply {
            putExtra(IntentRepo.URI, uri)
            putExtra(IntentRepo.FILE_PATH, file_path)
            action = IntentRepo.ACTION_NEW_INTENT
        }
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        //启动
        startActivity(intent)
    }


    //启动音乐页面
    @OptIn(UnstableApi::class)
    private fun startMusicPage(uri: Uri) {
        //越权设置音频
        PlayerSingleton.setMediaItem(uri = uri,playWhenReady = true)

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