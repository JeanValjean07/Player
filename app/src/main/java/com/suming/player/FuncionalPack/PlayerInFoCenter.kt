package com.suming.player.FuncionalPack

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.suming.player.DataPack.MediaInfo
import com.suming.player.PlayerSingleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("unused")
object PlayerInFoCenter {

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "PlayerInFoCenter: $msg")
        }
    }

    //承担播放状态和媒体信息

    const val mediaType_Music = "music"
    const val mediaType_Video = "video"

    //是否已设置媒体
    var state_item_set: Boolean = false
    //是否正在播放
    var state_isPlaying: Boolean = false

    //当前播放媒体信息
    var MediaInfoPackage: MediaInfo? = null

    //使用uri作为唯一可观察的标签,供外部观察媒体切换事件
    private val _uriString = MutableStateFlow("")
    val uriString: StateFlow<String> = _uriString.asStateFlow()

    //播放状态(播放,暂停)也可观察
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()


    //更新当前媒体链接(必须在其他信息完成替换后才触发观察事件变更)
    fun updateObservableUriString(newUriString: String){
        _uriString.value = newUriString
    }
    //更新播放状态
    fun updateObservableIsPlaying(newIsPlaying: Boolean){
        _isPlaying.value = newIsPlaying
    }



    //接收信息解码器传入的媒体信息
    fun setMediaInfoPack(MediaInfoPack: MediaInfo) {
        this.MediaInfoPackage = MediaInfoPack
        //更新内部可观察标签
        updateObservableUriString(MediaInfoPack.MediaInfo_MediaUriString)
    }





    //外部获取信息
    //检查媒体,媒体不同时,先读取新的信息
    const val uriString_for_check_null = ""
    fun checkMediaInfoPack(context: Context,uriString: String): Boolean{
        if (uriString != uriString_for_check_null){
            //更新一次信息
            val (success, _) = MediaInfoRetriever.retrieveMediaInfo(context,uriString.toUri())

            if(!success) {
                consoleLog("获取媒体信息失败")
                return false
            }
        }
        return true
    }
    //获取视频宽高比(aspect ratio 宽/高)
    fun getMediaAspectRatio(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,Float> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if(!success){
            return Pair(false,1f)
        }

        //获取宽高
        val MediaInfo_Video_Width = MediaInfoPackage!!.MediaInfo_Video_Width
        val MediaInfo_Video_Height = MediaInfoPackage!!.MediaInfo_Video_Height

        //计算视频宽高比
        val aspectRatio = MediaInfo_Video_Width.toFloat() / MediaInfo_Video_Height.toFloat()

        return Pair(false,aspectRatio)
    }
    //获取当前媒体信息完整包
    fun getMediaInfoPack(): MediaInfo? {

        return MediaInfoPackage
    }
    //只返回首页微型播放器需要的信息迷你包(uriNumOnly,fileName,artist)
    fun getMediaInfoMiniPack(): Triple<String, String, String>? {
        if (MediaInfoPackage == null) {
            return null
        }

        //从MediaInfo中提取三项信息(uriNumOnly,fileName,artist)
        val MediaInfo_uriNumOnly = MediaInfoPackage!!.MediaInfo_MediaUniqueID
        val MediaInfo_FileName = MediaInfoPackage!!.MediaInfo_FileName
        val MediaInfo_MediaArtist = MediaInfoPackage!!.MediaInfo_MediaArtist


        return Triple(MediaInfo_uriNumOnly, MediaInfo_FileName, MediaInfo_MediaArtist)
    }
    //获取当前媒体的播放进度
    @OptIn(UnstableApi::class)
    fun getEnginCurrentProgress(): Long {
        consoleLog("视频进度应向播放器引擎读取")
        val progress = PlayerSingleton.getEnginCurrentProgress()

        return progress
    }
    //获取当前媒体的唯一ID
    fun getMediaUniqueID(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaUniqueID = MediaInfoPackage!!.MediaInfo_MediaUniqueID

        return Pair(false, MediaInfo_MediaUniqueID)
    }
    //获取当前媒体的uriNumOnly
    fun getMediaUriNumOnly(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean, Long> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,114514L)
        }

        val MediaInfo_MediaUriNumOnly = MediaInfoPackage!!.MediaInfo_MediaUriNumOnly

        return Pair(false, MediaInfo_MediaUriNumOnly)
    }
    //获取当前媒体的标准链接
    fun getMediaUriStandard(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean, String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaUriStandard = MediaInfoPackage!!.MediaInfo_MediaUriStandard

        return Pair(false, MediaInfo_MediaUriStandard)
    }
    //获取当前媒体的uriString
    fun getMediaUriString(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaUriString = MediaInfoPackage!!.MediaInfo_MediaUriString

        return Pair(false, MediaInfo_MediaUriString)
    }
    //获取媒体是视频还是音乐
    fun getMediaInfoType(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaType = MediaInfoPackage!!.MediaInfo_MediaType

        if (MediaInfo_MediaType != mediaType_Video && MediaInfo_MediaType != mediaType_Music){
            return Pair(false,"哎呀,骇亖我力")
        }

        return Pair(false, MediaInfo_MediaType)
    }
    //获取当前媒体的文件名
    fun getMediaFileName(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaFileName = MediaInfoPackage!!.MediaInfo_FileName

        return Pair(false, MediaInfo_MediaFileName)
    }
    //获取当前媒体的艺术家
    fun getMediaArtist(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean,String> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,"哎呀,骇亖我力")
        }

        val MediaInfo_MediaArtist = MediaInfoPackage!!.MediaInfo_MediaArtist

        return Pair(false, MediaInfo_MediaArtist)
    }
    //获取当前媒体的总时长
    fun getMediaDuration(context: Context,uriStringForCheck: String = uriString_for_check_null): Pair<Boolean, Long> {
        val success = checkMediaInfoPack(context,uriStringForCheck)
        if (!success){
            return Pair(false,-114514L)
        }

        val MediaInfo_MediaDuration = MediaInfoPackage!!.MediaInfo_Duration

        return Pair(false, MediaInfo_MediaDuration)
    }
    fun getMediaDuration(): Long {

        return MediaInfoPackage?.MediaInfo_Duration ?: -114514L
    }
    //获取数据库ID
    fun getItemDataBaseID(): String {
        return MediaInfoPackage?.MediaInfo_DataBaseID ?: ""
    }

    //判断传入的链接是否为正在播放的项(数据缓存)
    fun isthisUriOngoing(context:Context,uriNeedCheck: Uri): Boolean {
        if (MediaInfoPackage == null){
            return false
        }

        val MediaInfo_MediaUriStandard = MediaInfoPackage!!.MediaInfo_MediaUriStandard
        //如果传入标准链接,就直接对比标准链接
        if (MediaUriManager.isMediaUriStandard(uriNeedCheck)){

            return uriNeedCheck.toString() == MediaInfo_MediaUriStandard
        }
        //若不是标准链接,先转成标准链接,再对比
        val standardUriNeedCheck = MediaUriManager.getStandardMediaUri(uriNeedCheck,context)

        return standardUriNeedCheck.toString() == MediaInfo_MediaUriStandard
    }


    //其他外部控制
    //清除当前信息包(疑似没用过)
    fun clearMediaInfoPack(context: Context) {
        MediaInfoPackage = null

    }





























}