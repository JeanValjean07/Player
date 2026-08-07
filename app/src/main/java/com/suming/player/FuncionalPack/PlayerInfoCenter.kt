package com.suming.player.FuncionalPack

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.suming.player.PlayerSingleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress() //"unused"
object PlayerInfoCenter {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = false) {
        if (mark) {
            Log.d("SuMing", "PlayerInFoCenter: $msg")
        }
    }


    //字段
    private const val Audio = MediaType.Audio
    private const val Video = MediaType.Video
    private const val Undefined = MediaType.Undefined
    private const val Undefined_String = ""


    //完整数据类
    data class MediaInfo(
        var MediaInfo_MediaUniqueID: String,
        var MediaInfo_DataBaseID : String,
        var MediaInfo_MediaUri: Uri,
        var MediaInfo_MediaUriString : String,
        var MediaInfo_MediaUriStandard : String,
        var MediaInfo_MediaUriNumOnly : Long,
        //
        var MediaInfo_MediaType : String,
        var MediaInfo_AbsolutePath : String,
        var MediaInfo_FileName : String,
        var MediaInfo_MediaTitle : String,
        var MediaInfo_MediaArtist : String,
        //
        var MediaInfo_Duration : Long,
        //
        var MediaInfo_Video_Width : Int,
        var MediaInfo_Video_Height : Int,
        //
        var MediaInfo_RealFps: Float = 0f


    )




    //当前播放媒体信息
    var MediaInfoPackage: MediaInfo? = null

    //使用uri作为唯一可观察的标签,供外部观察媒体切换事件
    private val _uriString = MutableStateFlow("")
    val uriString: StateFlow<String> = _uriString.asStateFlow()







    //接收信息解码器传入的媒体信息
    fun setMediaInfoPack(MediaInfoPack: MediaInfo) {
        MediaInfoPackage = MediaInfoPack
        consoleLog("setMediaInfoPack 设置媒体信息: ${MediaInfoPack.MediaInfo_MediaUriString}")
        current_uriString = MediaInfoPack.MediaInfo_MediaUriString
        //更新内部可观察标签
        updateObservableUriString(MediaInfoPack.MediaInfo_MediaUriString)
        //更新可观察数据类
        updateObservableMediaItem(ObservableMediaItem(
            MediaInfo_MediaUriString = MediaInfoPack.MediaInfo_MediaUriString,
            MediaInfo_MediaType = MediaInfoPack.MediaInfo_MediaType,
        ))
    }

    //设置真实帧率值
    fun setMediaFps(fps:Float){
        MediaInfoPackage?.MediaInfo_RealFps = fps
    }



    //外部获取信息
    //检查媒体,媒体不同时,先读取新的信息
    const val uriString_for_check_null = ""
    private var current_uriString = ""
    fun compareUriAndUpdate(context: Context,uriString: String): Boolean{
        if (uriString != current_uriString){
            //更新一次信息
            val (success, MediaInfoPack) = MediaInfoRetriever.retrieveMediaInfo(context,uriString.toUri())
            if (success) {
                setMediaInfoPack(MediaInfoPack)
                return true
            }else{
                consoleLog("获取媒体信息失败")
                return false
            }
        }
        return true
    }
    //获取视频宽高比(返回默认保底值1)
    fun getMediaAspectRatio(): Float {
        //获取宽高
        val MediaInfo_Video_Width = MediaInfoPackage?.MediaInfo_Video_Width?:1
        val MediaInfo_Video_Height = MediaInfoPackage?.MediaInfo_Video_Height?:1

        //计算视频宽高比
        val aspectRatio = MediaInfo_Video_Width.toFloat() / MediaInfo_Video_Height.toFloat()

        return aspectRatio
    }
    //获取当前媒体信息完整包
    fun getMediaInfoPack(): MediaInfo? {

        return MediaInfoPackage
    }
    //只返回首页微型播放器需要的信息迷你包(uriNumOnly,fileName,artist)(默认值为空)
    fun getMediaInfoMiniPack(): Triple<String, String, String> {
        //从MediaInfo中提取三项信息(uriNumOnly,fileName,artist)
        val MediaInfo_uriNumOnly = MediaInfoPackage?.MediaInfo_MediaUniqueID?:""
        val MediaInfo_FileName = MediaInfoPackage?.MediaInfo_FileName?:""
        val MediaInfo_MediaArtist = MediaInfoPackage?.MediaInfo_MediaArtist?:""


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
    fun getMediaUniqueID(): String {
        val MediaInfo_MediaUniqueID = MediaInfoPackage?.MediaInfo_MediaUniqueID?:""

        return MediaInfo_MediaUniqueID
    }
    //获取当前媒体的uriNumOnly
    fun getMediaUriNumOnly(): Long {
        val MediaInfo_MediaUriNumOnly = MediaInfoPackage?.MediaInfo_MediaUriNumOnly?:0L

        return MediaInfo_MediaUriNumOnly
    }
    //获取当前媒体的标准链接
    fun getMediaUriStandard(): String {
        val MediaInfo_MediaUriStandard = MediaInfoPackage?.MediaInfo_MediaUriStandard?:""

        return MediaInfo_MediaUriStandard
    }
    //获取当前媒体的uriString
    fun getMediaUriString(): String {
        val MediaInfo_MediaUriString = MediaInfoPackage?.MediaInfo_MediaUriString?:""

        return MediaInfo_MediaUriString
    }
    //获取媒体是视频还是音乐
    fun getMediaInfoType(): String {
        //尝试获取类型
        val MediaInfo_MediaType = MediaInfoPackage?.MediaInfo_MediaType ?: ""
        //检查类型是否合法
        if (MediaInfo_MediaType != MediaType.Video && MediaInfo_MediaType != MediaType.Audio){
            return "哎呀,骇亖我力"
        }

        return MediaInfo_MediaType
    }
    //获取当前媒体的文件名
    fun getMediaFileName(): String {
        val MediaInfo_MediaFileName = MediaInfoPackage?.MediaInfo_FileName?:""

        return MediaInfo_MediaFileName
    }
    //获取当前媒体的艺术家
    fun getMediaArtist(): String {
        val MediaInfo_MediaArtist = MediaInfoPackage?.MediaInfo_MediaArtist?:""

        return MediaInfo_MediaArtist
    }
    //获取当前媒体的总时长
    fun getMediaDuration(): Long {
        val MediaInfo_MediaDuration = MediaInfoPackage?.MediaInfo_Duration?:0L

        return MediaInfo_MediaDuration
    }
    //获取数据库ID
    fun getItemDataBaseID(): String {
        return MediaInfoPackage?.MediaInfo_DataBaseID ?: ""
    }
    //获取绝对路径
    fun getMediaAbsolutePath(): String {
        return MediaInfoPackage?.MediaInfo_AbsolutePath ?: ""
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




    //清除当前媒体信息(记得清除解码器链接缓存)
    fun clearCurrentMediaInfo() {
        MediaInfoPackage = null
        //清除解码器链接缓存
        MediaInfoRetriever.clearRetrieverUriCache()
        //修改可观察标记
        updateObservableUriString("")
        updateObservableMediaItem(ObservableMediaItem(
            MediaInfo_MediaUriString = Undefined,
            MediaInfo_MediaType = MediaType.Undefined,
        ))

    }

























    private fun TrashCan(){
        /*
        //专供观察数据类
        data class ObservableMediaItem(
            var MediaInfo_MediaUriString: String = Undefined,
            var MediaInfo_MediaType: String = MediaType.Undefined,
        )
        //可观察数据类实例
        private val _observableMediaItem = MutableStateFlow(ObservableMediaItem())
        val observableMediaItem: StateFlow<ObservableMediaItem> = _observableMediaItem.asStateFlow()
        //更新可观察数据类实例
        fun updateObservableMediaItem(newObservableMediaItem: ObservableMediaItem){
            _observableMediaItem.value = newObservableMediaItem
        }

        //可观察播放/暂停状态
        private val _isPlaying = MutableStateFlow(false)
        val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()





        //是否已设置媒体
        var state_item_set: Boolean = false
        //是否正在播放
        var state_isPlaying: Boolean = false

        //更新当前媒体链接(必须在其他信息完成替换后才触发观察事件变更)
    fun updateObservableUriString(newUriString: String){
        _uriString.value = newUriString
    }
    //更新播放状态
    fun updateObservableIsPlaying(newIsPlaying: Boolean){
        _isPlaying.value = newIsPlaying
    }


         */
    }


}