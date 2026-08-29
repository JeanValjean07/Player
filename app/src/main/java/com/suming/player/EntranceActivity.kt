package com.suming.player

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.suming.player.FuncionalPack.SOURCE_CODE
import java.io.File
import java.net.URLDecoder
import kotlin.math.absoluteValue

@Suppress("NewApi")
class EntranceActivity : AppCompatActivity(){
    companion object {
        private const val REQUEST_CODE_OPEN_DIRECTORY = 1001
    }

    //空字段
    private val Undefined = ""
    //ctx
    private val context = this@EntranceActivity
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()
    //PrivacyPermissionHelper
    private val PrivacyPermissionHelper: PrivacyPermissionHelper = PrivacyPermissionHelper()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //检查权限
        val isAllFilesAccessGranted = PrivacyPermissionHelper.isAllFilesAccessGranted()


        //持久化URI权限到Activity上下文
        val URI_U_O = intent.data ?: Uri.EMPTY
        //consoleLog("URI_U_O = $URI_U_O")
        //尝试持久化URI权限到Activity上下文 //TODO 测试别的播放器能不能放
        try{
            context.grantUriPermission(packageName, URI_U_O, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }catch(e: Exception){
            if (!isAllFilesAccessGranted){
                fail("请开启所有文件访问权限")
                return
            }else{
                //fail("无法播放")
                //return
            }
        }


        //主业务
        mainBusiness()

        //自动关闭Activity
        finish()

    }



    //主业务
    private fun mainBusiness(){
        //提取URI  URI_U_O = URI_Original
        val (URI_U_O,SOURCE) = detectOriginalInfo_fromIntent(intent)
        val URI_S_O = URI_U_O.toString()   //URI_S_O = URI_String_Original
        consoleLog("URI_S_O = $URI_S_O, SOURCE = $SOURCE")


        //根据 SOURCE_CODE 处理
        when(SOURCE){
                //以新链接为目标
                SOURCE_CODE.VIEW,SOURCE_CODE.NORMAL -> {
                    processOutSource(URI_S_O, SOURCE)
                }
                //以正在播放项为目标
                SOURCE_CODE.PENDING -> {
                    processPending()
                }
                //未知来源
                else -> {
                    fail("页面打开失败(启动来源未知)")
                }
            }

    }

    //从Intent提取 URI 和 SOURCE (SOURCE_CODE = 1:分享和从其他应用打开  2:pendingIntent(通知中心媒体会话))
    private fun detectOriginalInfo_fromIntent(intent: Intent): Pair<Uri, Int> {
        when (intent.action) {
            //系统面板：分享
            Intent.ACTION_SEND -> {
                val URI_U_O = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?: Uri.EMPTY

                return Pair(URI_U_O, SOURCE_CODE.SHARE)
            }
            //系统面板：选择其他应用打开
            Intent.ACTION_VIEW -> {
                val URI_U_O = intent.data ?: Uri.EMPTY

                return Pair(URI_U_O, SOURCE_CODE.VIEW)
            }
            //正常打开
            else -> {
                //来自pendingIntent
                if (intent.getStringExtra(IntentRepo.SOURCE) == SOURCE_CODE.SOURCE_Pending){
                    //来自pendingIntent时直接拉起播放页,不关注也不传入链接

                    return Pair(Uri.EMPTY, SOURCE_CODE.PENDING)
                }
                //来自常规启动
                else{
                    //来自常规启动时,不需要关注链接是否存在,由播放页处理
                    val URI_U_O = IntentCompat.getParcelableExtra(intent, IntentRepo.URI, Uri::class.java)?: Uri.EMPTY

                    return Pair(URI_U_O, SOURCE_CODE.NORMAL)
                }
            }
        }
    }

    //以新链接为目标(注意:以下传入链接类型可被ExoPlayer直接播放:1. MediaStore详细表链接 2.文件路径。警告：FileProvider链接不一定能直接播放,MediaStore文件表链接也不行)
    @OptIn(UnstableApi::class)
    private fun processOutSource(URI_S_O: String, SOURCE_CODE: Int){
        if (URI_S_O != Undefined){
            //分支-新链接不为空

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

            //缓存URI_U_O
            val URI_U_O = URI_S_O.toUri()

            //检查 URI 类型
            val uriTypeMode = MediaUriManager.detectMediaUriTypeMode(URI_U_O)
            when (uriTypeMode){
                //MediaStore详细表链接(示例 content://media/external/video/media/2599,content://media/external/audio/media/2537)
                MediaUriManager.uriType_media_store_detail -> {

                    //处理详细表URI
                    executeByUriType_uriType_media_store_detail(URI_S_O,SOURCE_CODE)

                }
                //MediaStore文件表链接(示例 content://media/external/file/2622)
                MediaUriManager.uriType_media_store_file -> {

                    //
                    executeByUriType_uriType_media_store_file(URI_S_O,SOURCE_CODE)

                    return
                }
                //包含文件路径链接(示例 content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4 )
                MediaUriManager.uriType_contain_file_path -> {

                    //处理文件路径的URI
                    executeByUriType_uriType_contain_file_path(URI_S_O,SOURCE_CODE)

                }
                //其他ContentProvider URI
                MediaUriManager.uriType_other_content_provider -> {

                    //处理其他ContentProvider URI
                    executeByUriType_uriType_other_content_provider(URI_U_O,SOURCE_CODE)

                }
                //链接无法解析
                MediaUriManager.uriType_null -> {

                    fail("播放失败(无法解析链接:$URI_S_O)")
                }
            }

        }else{
            //分支-新链接为空

            fail("播放失败(未传入媒体基本信息)")
        }
    }
    //以正在播放项为目标
    @OptIn(UnstableApi::class)
    private fun processPending(){
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
                        startVideoPage_selfDetectStyle(uri, Undefined)

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



    //详细URI(示例 content://media/external/video/media/2624)
    private fun executeByUriType_uriType_media_store_detail(URI_S_O: String,SOURCE: Int){
        //尝试解码获得媒体类型
        val (result,MediaItemForPlay,_) = MediaInfoRetriever.retrieveMediaInfo(
            context,
            URI_S_O,
            Undefined,
            MediaUriManager.uriType_media_store_detail,
            Undefined
        )
        //失败条件检查
        when(result){
            ActivityResultConnector.retriever_error -> {
                fail("解码失败")
                return
            }
            ActivityResultConnector.retriever_get_type_failed -> {
                fail("格式获取失败")
                return
            }
            ActivityResultConnector.retriever_type_not_support -> {
                fail("格式不支持")
                return
            }
        }
        //获取媒体类型
        val mediaType = MediaItemForPlay.media_SPECIFIC_MediaType
        if (mediaType == MediaType.Undefined){
            fail("格式不支持")
            return
        }
        //获取文件路径
        val file_path = MediaItemForPlay.file_path

        //启动播放页
        startPage_selfDetectMediaType(URI_S_O.toUri(),file_path,SOURCE,mediaType)
    }

    //文件表URI(示例 content://media/external/file/2622)
    private fun executeByUriType_uriType_media_store_file(URI_S_O: String,SOURCE: Int){
        consoleLog("文件表URI -URI_S_O = $URI_S_O, SOURCE = $SOURCE")

        //尝试解码获得媒体类型
        val (result,MediaItemForPlay,_) = MediaInfoRetriever.retrieveMediaInfo(
            context,
            URI_S_O,
            Undefined,
            MediaUriManager.uriType_media_store_detail,
            Undefined
        )
        //失败条件检查
        when(result){
            ActivityResultConnector.retriever_error -> {
                fail("解码失败")
                return
            }
            ActivityResultConnector.retriever_get_type_failed -> {
                fail("格式获取失败")
                return
            }
            ActivityResultConnector.retriever_type_not_support -> {
                fail("格式不支持")
                return
            }
        }
        //获取媒体类型
        val mediaType = MediaItemForPlay.media_SPECIFIC_MediaType
        if (mediaType == MediaType.Undefined){
            fail("格式不支持")
            return
        }
        //获取文件路径
        val file_path = MediaItemForPlay.file_path

        //启动播放页
        startPage_selfDetectMediaType(URI_S_O.toUri(),file_path,SOURCE,mediaType)

    }

    //包含文件路径 (示例 content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4)
    private fun executeByUriType_uriType_contain_file_path(URI_S_O: String,SOURCE: Int){
        //对照表
        //URI_S_O = URI String Original
        //file_path_c_u = file_path clean undecoded 已经掐头去尾,但未移除URI编码 /storage/emulated/0/Movies/%E7%B2%BE%E9%80%89/1-nomedia/Jessica%20Starling.mp4
        //file_path_c_d = file_path clean decoded 已经掐头去尾,已移除URI编码 /storage/emulated/0/Movies/精选/1-nomedia/Jessica Starling.mp4
        //URI_S_MS = URI String MediaStore Standard 标准格式的URI,用于播放文件 content://media/external/video/media/2599
        //URI_U_FP = URI for Play 用于播放的URI
        //URI_S_FP = URI String for Play 用于播放的URI的字符串(方便各种处理)
        //URI_U_FR = URI for Retrieve 用于解码的URI
        //URI_S_FR = URI String for Retrieve 用于解码的URI的字符串(方便各种处理)


        //获取文件路径字段
        val file_path_c_u = "/storage/emulated/0/" + URI_S_O.substringAfter("storage/emulated/0/").substringBefore('?')
        val file_path_c_d = MediaUriManager.decode_file_path(file_path_c_u)

        //检查对应文件是否存在
        val file = File(file_path_c_d)
        if (!file.exists()){ fail("播放失败(文件不存在)") ; return }

        //尝试查表获取标准链接(必须获取到标准链接才启动播放,否则,需要走提前解码分支)
        val URI_S_MS = MediaUriManager.detect_FilePath(file_path_c_d,context) //URI String MediaStore Standard

        //决策出用于播放的URI
        val URI_U_FP = if (URI_S_MS != Undefined){
            URI_S_MS.toUri()
        }else{
            URI_S_O.toUri()
        }
        //决策出用于解码的URI
        val URI_S_FR = if (URI_S_MS != Undefined){
            URI_S_MS
        }else{
            URI_S_O
        }
        consoleLog("executeByUriType_uriType_contain_file_path -URI_S_FR: $URI_S_FR")
        //尝试解码+失败条件检查
        val (_,MediaItemForPlay,_) = MediaInfoRetriever.retrieveMediaInfo(
            context,
            URI_S_FR,
            file_path_c_d,
            MediaUriManager.uriType_contain_file_path,
            URI_S_MS
        )

        //由于前面已经检查过文件是否存在,这里不能解码不代表文件不存在,不代表不能播放
        //获取媒体类型(用媒体类型获取来判断是否解码成功)
        val mediaType = MediaItemForPlay.media_SPECIFIC_MediaType
        if (mediaType == Undefined){
            //使用本地备用解码器解码
            val (_,MediaItemForPlay,_) = retrieveMediaInfo(
                this,
                URI_S_FR,
                file_path_c_d,
                MediaUriManager.uriType_contain_file_path,
                URI_S_MS
            )
            val mediaType = MediaItemForPlay.media_SPECIFIC_MediaType
            if (mediaType == Undefined){
                fail("非常不理想的错误(本地备用解码器解码失败)")
                return
            }else{
                //启动播放页
                startPage_selfDetectMediaType(URI_U_FP,file_path_c_d,SOURCE,mediaType)
            }

        }else{

            //启动播放页
            startPage_selfDetectMediaType(URI_U_FP,file_path_c_d,SOURCE,mediaType)


        }



    }

    //其他ContentProvider URI
    private fun executeByUriType_uriType_other_content_provider(URI_U_O: Uri,SOURCE: Int){
        //探测一下暴露了哪些可读的列 projection = null 获取所有列
        try {
            contentResolver.query(URI_U_O,null,null,null,null)?.use { cursor ->
                //
                cursor.columnNames.toList()

                consoleLog("executeByUriType_uriType_other_content_provider -columnNames: ${cursor.columnNames.toList()}")
                //
                //(0 until cursor.columnCount).map { cursor.getColumnName(it) }
            }
        }catch(e: Exception){
            consoleLog("executeByUriType_uriType_other_content_provider -无法查询该URI: $URI_U_O e:$e")
            fail("无法查询该URI: $URI_U_O e:$e")
        }

        try {
            contentResolver.openInputStream(URI_U_O)?.use { inputStream ->
                // 如果这里成功了，那就直接读
                consoleLog("成功读取文件流，大小: ${inputStream.available()}")
            }
        }catch(e: SecurityException){
            // 大概率还是会报权限错误
            consoleLog("无法通过ContentResolver读取，需要降级到方案A或B")
            fail("无法通过ContentResolver读取，需要降级到方案A或B")

        }

        try {
            contentResolver.openAssetFileDescriptor(URI_U_O, "r")?.use { afd ->
                // AssetFileDescriptor 包含额外信息
                val size = afd.length  // 文件大小
                val startOffset = afd.startOffset
                val declaredLength = afd.declaredLength

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

                val MediaInfo_MediaType_Original = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    ?: Undefined
                val MediaInfo_MediaType = when {
                    MediaInfo_MediaType_Original.contains("video") -> {
                        MediaType.Video
                    }
                    MediaInfo_MediaType_Original.contains("audio") -> {
                        MediaType.Audio
                    }
                    MediaInfo_MediaType_Original.isEmpty() -> Undefined
                    else -> MediaInfo_MediaType_Original

                }
                if (MediaInfo_MediaType == Undefined){
                    consoleLog("格式获取失败")
                    return
                }
                if (MediaInfo_MediaType != MediaType.Video && MediaInfo_MediaType != MediaType.Audio){
                    consoleLog("格式获取成功但不支持")
                    return
                }


                val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong()
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()

                consoleLog("视频时长: ${duration?.div(1000)}秒, 分辨率: ${width}x${height}")
                retriever.release()

                // 获取输入流
                val inputStream = afd.createInputStream()
                inputStream.use { stream ->
                    // 读取内容...
                    consoleLog("AssetFD方式 - 大小: $size, 偏移: $startOffset")
                }

                // 对于视频，可以直接使用
                // mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

                //启动播放页
                startPage_selfDetectMediaType(URI_U_O,Undefined,SOURCE,MediaInfo_MediaType)


                true
            } ?: false
        }catch(e: SecurityException){
            consoleLog("AssetFD方式权限不足: ${e.message}")
            fail("AssetFD方式权限不足: ${e.message}")
        }catch(e: Exception){
            consoleLog("AssetFD方式失败: ${e.message}")
            fail("AssetFD方式失败: ${e.message}")
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
        //consoleLog("startPage_selfDetectMediaType -uri: $uri -source: $source -mediaType: $mediaType")

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
        val intent = Intent(applicationContext, PlayerActivityNeo::class.java).apply {
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
        //启动播放器
        PlayerSingleton.getInitPlayer()
        //越权设置音频
        PlayerSingleton.setMediaItem(URI_UP = uri,playWhenReady = true)

        //构建intent
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        //启动
        startActivity(intent)
    }

    //本地解码器
    private fun retrieveMediaInfo(context: Context, URI_S_FR:String, file_path:String, UriTypeMode_e:String, URI_S_FP:String): Triple<String, MediaItemForPlay, String> {
        var retriever: MediaMetadataRetriever? = null
        //初始化retriever
        fun init_retriever(){
            retriever = MediaMetadataRetriever()
        }
        //释放retriever
        fun release_retriever(){
            retriever?.release()
            retriever = null
        }



        //缓存URI_S_FR
        val URI_U_FR = URI_S_FR.toUri()

        //启动retriever
        init_retriever()

        //尝试设置数据源
        try{
            //检查URI_S是否实际上是一个文件路径
            val is_file_path = MediaUriManager.spy_is_string_actually_a_file_path(URI_S_FR)
            if (is_file_path){
                consoleLog("本地解码器：使用文件路径解码 -URI_S_FR: $URI_S_FR")
                val file_path = URI_S_FR
                val URI_File = Uri.fromFile(File(file_path))

                val pfd = context.contentResolver.openFileDescriptor(URI_File, "r")
                if (pfd == null) {
                    consoleLog("无法获取文件描述符")
                    return Triple(ActivityResultConnector.retriever_error ,MediaItemForPlay(),Undefined)
                }else{
                    consoleLog("获取文件描述符成功")
                }
                //设置路径为数据源
                pfd.use { fd -> retriever?.setDataSource(fd.fileDescriptor) }

            }else{
                consoleLog("本地解码器：使用URI解码 -URI_S_FR: $URI_S_FR")
                //设置URI为数据源
                retriever?.setDataSource(context,URI_U_FR)
            }

        }catch(e: Exception){
            consoleLog("retrieveMediaInfo -发生错误:e:$e,message:${e.message}")

            return Triple(ActivityResultConnector.retriever_error ,MediaItemForPlay(),Undefined)
        }

        //尝试解码
        try{
            //获取媒体类型
            val MediaInfo_MediaType_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: Undefined
            val MediaInfo_MediaType = when {
                MediaInfo_MediaType_Original.contains("video") -> {
                    MediaType.Video
                }
                MediaInfo_MediaType_Original.contains("audio") -> {
                    MediaType.Audio
                }
                MediaInfo_MediaType_Original.isEmpty() -> Undefined
                else -> MediaInfo_MediaType_Original

            }
            if (MediaInfo_MediaType == Undefined){
                consoleLog("retrieveMediaInfo -使用 URI 解码时 发生错误: 格式获取失败")
                return Triple(ActivityResultConnector.retriever_get_type_failed ,MediaItemForPlay(),Undefined)
            }
            if (MediaInfo_MediaType != MediaType.Video && MediaInfo_MediaType != MediaType.Audio){
                consoleLog("retrieveMediaInfo -使用 URI 解码时 发生错误: 格式获取成功但不支持")
                return Triple(ActivityResultConnector.retriever_type_not_support,MediaItemForPlay(),Undefined)
            }
            //获取 URI 类型
            val UriTypeMode = if (UriTypeMode_e == Undefined){
                //consoleLog("retrieveMediaInfo -未传入 UriTypeMode, 本地尝试获取 UriTypeMode")
                MediaUriManager.detectMediaUriTypeMode(URI_U_FR)
            }else{
                UriTypeMode_e
            }
            //获取绝对路径
            val file_path =  if (file_path == Undefined){
                when(UriTypeMode){
                    MediaUriManager.uriType_media_store_detail -> {
                        GET_FilePath_From_MediaUri_SC1(context,URI_U_FR)
                    }
                    MediaUriManager.uriType_contain_file_path -> {
                        GET_FilePath_From_FileProviderURI(URI_U_FR)
                    }
                    else -> Undefined

                }
            }else{
                file_path
            }


            val MediaInfo_FileName = if((File(file_path)).name.isEmpty()){
                "未知媒体文件名"
            } else {
                (File(file_path)).name ?: "未知媒体文件名"
            }
            val MediaInfo_MediaTitle_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: Undefined
            val MediaInfo_MediaArtist_Original = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: Undefined
            val MediaInfo_Duration = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1L
            //过滤获取的信息
            val MediaInfo_MediaTitle = if (MediaInfo_MediaTitle_Original == Undefined){
                "未知媒体标题"
            }else{
                MediaInfo_MediaTitle_Original
            }
            val MediaInfo_MediaArtist = if (MediaInfo_MediaArtist_Original == Undefined){
                "未知艺术家"
            }else{
                MediaInfo_MediaArtist_Original
            }
            //视频专属
            val MediaInfo_VideoWidth = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLong() ?: 0L
            val MediaInfo_VideoHeight = retriever?.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLong() ?: 0L


            //检查链接格式(必须是标准 MediaStore URI 时才启用 NUM_ID 和 SPECIFIC_ID )
            val URI_STD = if (UriTypeMode == MediaUriManager.uriType_media_store_detail){
                MediaUriManager.GET_STD_MediaStoreURI_from_Any_URI(URI_S_FR,context)
            }else{
                //万一传入链接是个标准链接
                val is_URI_S_MS = MediaUriManager.spy_is_string_matches_a_MediaStore_S_URI(URI_S_FR)
                if (is_URI_S_MS){
                    URI_S_FR
                }else{
                    Undefined
                }
            }
            val NUM_ID = if (URI_STD != Undefined){
                URI_STD.split("/").last().toLong()
            }else{
                if (file_path != Undefined){
                    //TODO 这里有可能跟MediaStore自增ID冲突,只是概率极小
                    file_path.hashCode().absoluteValue.toLong()
                }else{
                    0L
                }
            }
            val SPECIFIC_ID = if (NUM_ID > 0){
                calculate_SPECIFIC_ID(MediaInfo_MediaType,NUM_ID.toString())
            }else{
                Undefined
            }

            //日志
               /*
            consoleLog("retrieveMediaInfo -使用 URI 解码 -结果：" +
                    "MediaInfo_MediaType: $MediaInfo_MediaType , " +
                    "file_path: $file_path , " +
                    "MediaInfo_FileName: $MediaInfo_FileName , " +
                    "MediaInfo_Duration: $MediaInfo_Duration , " +
                    "MediaInfo_MediaTitle: $MediaInfo_MediaTitle , " +
                    "MediaInfo_MediaArtist: $MediaInfo_MediaArtist , " +
                    "MediaInfo_VideoWidth: $MediaInfo_VideoWidth , " +
                    "MediaInfo_VideoHeight: $MediaInfo_VideoHeight , " +
                    "URI_STD: $URI_STD , " +
                    "NUM_ID: $NUM_ID , " +
                    "SPECIFIC_ID: $SPECIFIC_ID"
            )
               */

            //合成数据包
            val MediaInfoPack = MediaItemForPlay(
                media_api_SPECIFIC_ID = SPECIFIC_ID,
                media_api_NUM_ID = NUM_ID,
                media_api_dateAdded = 0,
                media_SPECIFIC_MediaType = MediaInfo_MediaType,
                URI_S_FP = URI_S_FP,
                file_path = file_path,
                file_name = MediaInfo_FileName,
                file_size = 0L,
                media_title = MediaInfo_MediaTitle,
                media_artist = MediaInfo_MediaArtist,
                media_durationMs = MediaInfo_Duration,
                media_format = "",

                //类型专属
                video_videoHeight = MediaInfo_VideoHeight,
                video_videoWidth = MediaInfo_VideoWidth,
                video_actualFPS = 0f,

                )

            return Triple(ActivityResultConnector.retriever_complete, MediaInfoPack,UriTypeMode)
        }catch(e: Exception){
            consoleLog("retrieveMediaInfo -使用 URI 解码 发生错误 $e")

            return Triple(ActivityResultConnector.retriever_error, MediaItemForPlay(),Undefined)
        }finally{
            release_retriever()
        }

    }
    //从URI获取文件路径(必须是详情URI 2种方案 1基于contentResolver 2基于)
    private fun GET_FilePath_From_MediaUri_SC1(context: Context, uri: Uri): String {
        var cursor: Cursor? = null
        return try {
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            val path = cursor?.let {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                it.moveToFirst()
                it.getString(columnIndex)
            }
            return path ?: Undefined
        }catch(e: Exception){
            consoleLog("GET_FilePath_From_MediaUri_SC1 -获取文件路径发生错误: $e")
            Undefined
        }finally{
            cursor?.close()
        }
    }
    //从FileProviderURI获取文件路径
    private fun GET_FilePath_From_FileProviderURI(URI: Uri): String {
        try{
            val path = URI.path ?: return MediaUriManager.Undefined

            val file_path = path.replace("/root", "") // 移除 /root 前缀
            val storageIndex = path.indexOf("/storage/emulated/")
            if (storageIndex != -1) {
                return path.substring(storageIndex)
            }

            return try {
                URLDecoder.decode(path, "UTF-8")
            }catch(e: Exception){
                consoleLog("GET_FilePath_From_FileProviderURI -解码URL编码的路径失败: $e")
                path
            }

        }catch(e: Exception){
            consoleLog("GET_FilePath_From_FileProviderURI -获取文件路径失败: $e")

            return MediaUriManager.Undefined
        }
    }
    //SPECIFIC_ID 计算器
    private val SPECIFIC_ID_SEPARATOR = "_"
    fun calculate_SPECIFIC_ID(mediaType: String, mediaNUMID: String): String{

        return "${mediaType}${SPECIFIC_ID_SEPARATOR}${mediaNUMID}"
    }



    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "EntranceActivity: $msg")
        }
    }


}