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
import com.suming.player.FuncionalPack.DeviceInfo
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
        //consoleLog("以新链接为目标 processOutSource ")
        if (targetUriString != Undefined){
            //分支-新链接不为空
            consoleLog("processOutSource 新链接不为空-原始链接: $targetUriString 来源标记:$source")
            //几种典型的targetUri:
            //MediaStore详细表链接(content://media/external/video/media/2599,content://media/external/audio/media/2537)
            //MediaStore文件表链接(content://media/external/file/2622) <- 一般是非公有文件夹和.nomedia文件夹出现这种链接
            //FileProvider链接:(不一定包含fileprovider字段,但一定包含storage/emulated/0字段)
            //content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4
            //content://com.coloros.filemanager/root/storage/emulated/0/Pictures/%E9%9F%B3%E4%B9%90%E8%A7%86%E9%A2%91/%E4%B8%8B%E5%B1%B1_DJ%E7%89%88.mp4
            //content://114514/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4

            //特殊链接:
            //华为相册私有：/storage/emulated/0/Pictures/音乐视频/天空.mp4?bgcolor=-920587

            //检查链接有效性和媒体类型(这个居然不需要任何权限就能查)
            val (success, mediaType) = MediaInfoRetriever.getUriValidAndMediaType(this,targetUriString)
            consoleLog("processOutSource-链接有效性检查结果: success:$success, mediaType:$mediaType")
            if (!success){ fail("播放失败(媒体无效)") ; return }

            //检查链接的权限级别(是否属于被降级的底权限链接)
            val uriTypeMode = MediaUriManager.detectMediaUriTypeMode(targetUriString.toUri())
            when (uriTypeMode){
                //MediaStore详细表链接(content://media/external/video/media/2599,content://media/external/audio/media/2537)
                MediaUriManager.uriType_media_store_detail -> {
                    //consoleLog("processOutSource -原始链接是MediaStore详细表链接: $targetUriString")

                    //启动播放页
                    startPage_selfDetectMediaType(targetUriString.toUri(), source,mediaType)

                }
                //MediaStore文件表链接(content://media/external/file/2622)
                MediaUriManager.uriType_media_store_file -> {
                    //consoleLog("processOutSource -原始链接是MediaStore文件表链接: $targetUriString")

                    //检查权限
                    val privacyPermissionHelper = PrivacyPermissionHelper()
                    val isAllFilesAccessGranted = privacyPermissionHelper.isAllFilesAccessGranted()
                    if (isAllFilesAccessGranted){
                        //启动播放(直接用File URI也可播放,无需再获取详细URI,而且根本也获取不到)
                        startPage_selfDetectMediaType(targetUriString.toUri(), source, mediaType)

                    }else{
                        fail("播放失败(请授权所有文件访问权限)")
                        return
                    }

                }

                //FileProvider链接(content://fileprovider/filemanager/fileprovider/filemanager, content://bin.mt.plus.fp/storage/emulated/0/DCIM/xxxxxxxoriginal.mp4)
                MediaUriManager.uriType_file_provider -> {
                    //consoleLog("processOutSource -原始链接是FileProvider链接: $targetUriString")

                    //将FileProvider链接转换为标准链接
                    val standardUri = MediaUriManager.convert_FileManagerFileURI_to_MediaStoreMediaURI(this, targetUriString.toUri()).first
                    //consoleLog("processOutSource-转换后的标准链接: $standardUri")
                    if (standardUri == Uri.EMPTY){
                        if (DeviceInfo.AndroidVersion == 29){
                            fail("播放失败(安卓10无法访问非公有文件夹和.nomedia文件夹)")
                        }else{
                            fail("播放失败(标准链接转换失败:$targetUriString)")
                        }
                    }else{
                        //成功转换出详细表标准链接(格式:content://media/external/video/media/2624)

                        //启动播放页
                        startPage_selfDetectMediaType(standardUri, source,mediaType)
                    }

                }

                //链接无法解析
                MediaUriManager.uriType_null -> {
                    consoleLog("processOutSource -uriTypeMode -无法解析链接: $targetUriString")

                    fail("播放失败(无法解析链接:$targetUriString)")
                }

                //特殊处理
                MediaUriManager.uriType_special -> {
                    consoleLog("processOutSource -uriTypeMode -特殊链接: $targetUriString")

                    //对特殊链接进行处理(转换为FileProvider链接)
                    val processedUri = MediaUriManager.processSpecialUri(targetUriString)
                    consoleLog("processOutSource -uriTypeMode -特殊链接处理结果: $processedUri")
                    if (processedUri == Uri.EMPTY){ fail("播放失败(特殊链接处理失败)") ; return }

                    //将FileProvider链接转换为标准链接
                    val standardUri = MediaUriManager.convert_FileManagerFileURI_to_MediaStoreMediaURI(this, targetUriString.toUri()).first
                    consoleLog("processOutSource-转换后的标准链接: $standardUri")
                    if (standardUri == Uri.EMPTY){
                        fail("播放失败(标准链接转换失败:$targetUriString)")
                    }else{
                        //成功转换出详细表标准链接(格式:content://media/external/video/media/2624)

                        //启动播放页
                        startPage_selfDetectMediaType(standardUri, source,mediaType)
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



    //使用SAF申请某个文件夹的访问
    fun requestFolderAccessViaSAF(folderPath: String) {
        try {
            // 获取文件夹的父路径，确保用户选择的是视频所在目录
            // 例如: /storage/emulated/0/Movies/精选/SL/bbbbb.mp4
            // 我们请求访问: /storage/emulated/0/Movies/精选/SL/
            val file = File(folderPath)
            val parentDir = file.parentFile ?: return

            // 构建一个提示，让用户知道我们想访问哪个文件夹
            val folderName = parentDir.name

            // 使用 Intent.ACTION_OPEN_DOCUMENT_TREE 请求文件夹访问权限
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // 可选：提示用户选择特定文件夹
                putExtra(Intent.EXTRA_TITLE, "请选择 \"$folderName\" 文件夹")

                // 可选：设置初始目录（但注意：这个在 Android 上不一定生效）
                // 可以尝试引导用户到正确的目录
                // Android 8+ 支持设置初始目录
                // 但实际效果取决于系统
            }

            // 存储目标路径，以便在回调中验证
            val key = "requested_folder_path"
            // 可以用临时变量或保存在类属性中
            val requestedFolderPath = parentDir.absolutePath

            startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY)

            consoleLog("请求 SAF 文件夹授权: ${parentDir.absolutePath}")
        } catch (e: Exception) {
            consoleLog("请求 SAF 授权失败: $e")
            // 降级处理：提示用户手动操作
            //showManualHelpDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_OPEN_DIRECTORY -> {
                if (resultCode == RESULT_OK && data != null) {
                    // 用户选择了文件夹
                    val treeUri = data.data

                    if (treeUri != null) {
                        // 获取持久化访问权限
                        try {
                            contentResolver.takePersistableUriPermission(
                                treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )

                            consoleLog("获得文件夹访问权限: $treeUri")



                        } catch (e: SecurityException) {
                            consoleLog("无法持久化权限: $e")

                        }
                    }
                } else {
                    // 用户拒绝了权限
                    consoleLog("用户拒绝了文件夹访问")

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