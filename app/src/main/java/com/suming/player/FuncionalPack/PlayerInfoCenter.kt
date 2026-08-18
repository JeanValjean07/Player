package com.suming.player.FuncionalPack

import android.util.Log
import com.suming.player.DataPack.DataClassForPlay.MediaItemForPlay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Suppress("unused") //"unused"
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
    private const val Undefined = ""





    //当前播放媒体信息
    var CURRENT_MediaItemPackage: MediaItemForPlay? = null
    var LAST_MediaItemPackage: MediaItemForPlay? = null


    //可观察数据类
    private var _observableMediaItem = MutableStateFlow(MediaItemForPlay())
    var observableMediaItem: StateFlow<MediaItemForPlay> = _observableMediaItem.asStateFlow()
    fun updateObservableMediaItem(MediaItemPack: MediaItemForPlay){
        _observableMediaItem.value = MediaItemPack
    }
    //可观察是否正在播放
    private var _observableIsPlaying = MutableStateFlow(false)
    var observableIsPlaying: StateFlow<Boolean> = _observableIsPlaying.asStateFlow()
    fun updateObservableIsPlaying(isPlaying: Boolean){
        _observableIsPlaying.value = isPlaying
    }





    //接收信息解码器传入的媒体信息
    fun setMediaInfoPack(MediaInfoPack: MediaItemForPlay) {
        //缓存旧数据
        LAST_MediaItemPackage = CURRENT_MediaItemPackage
        //缓存新数据
        CURRENT_MediaItemPackage = MediaInfoPack
        //更新可观察数据类
        updateObservableMediaItem(MediaInfoPack)

    }

    //仅能外部后期传入的项
    //设置真实帧率值
    fun SET_Media_ActualFPS(fps:Float){
        CURRENT_MediaItemPackage?.video_actualFPS = fps
    }
    //写入cache包
    fun SET_Media_CachePack(SPECIFIC_ID:String,mediaType:String,NUM_ID:Long,uriString:String,FileName:String,MediaArtist:String){
        val newMediaPack = MediaItemForPlay(
            media_api_SPECIFIC_ID = SPECIFIC_ID,
            media_SPECIFIC_MediaType = mediaType,
            media_api_NUM_ID = NUM_ID,
            content_uriString = uriString,
            file_name = FileName,
            media_artist = MediaArtist,

            isCache = true,
        )
        //写入本地数据包
        CURRENT_MediaItemPackage = newMediaPack
        //更新可观察数据类
        updateObservableMediaItem(newMediaPack)


    }



    //获取信息
    //获取isCache状态
    fun GET_Media_isCache(): Boolean {

        return CURRENT_MediaItemPackage?.isCache ?: false
    }
    //获取当前媒体信息完整包
    fun GET_Media_FullMediaInfoPack(): MediaItemForPlay? {

        return CURRENT_MediaItemPackage
    }
    //获取上一个媒体信息完整包
    fun GET_LAST_MediaInfoPack(): MediaItemForPlay? {

        return LAST_MediaItemPackage
    }
    //只返回首页微型播放器需要的信息迷你包
    fun GET_Media_MiniView_Pack(): Triple<String, String, String> {
        //从MediaInfo中提取三项信息
        val SPECIFIC_ID = CURRENT_MediaItemPackage?.media_api_SPECIFIC_ID ?: Undefined
        val FileName = CURRENT_MediaItemPackage?.file_name ?: Undefined
        val MediaArtist = CURRENT_MediaItemPackage?.media_artist ?: Undefined


        return Triple(SPECIFIC_ID, FileName, MediaArtist)
    }
    //获取当前媒体的 SPECIFIC_ID
    fun GET_Media_SPECIFIC_ID(): String {
        val Media_SPECIFIC_ID = CURRENT_MediaItemPackage?.media_api_SPECIFIC_ID ?: Undefined

        return Media_SPECIFIC_ID
    }
    //获取当前媒体的 NUM_ID
    fun GET_Media_NUM_ID(): Long {
        val Media_NUM_ID = CURRENT_MediaItemPackage?.media_api_NUM_ID ?: 0L

        return Media_NUM_ID
    }
    //获取当前媒体的标准链接
    fun GET_Media_UriStandard(): String {
        val MediaInfo_MediaUriStandard = CURRENT_MediaItemPackage?.content_uriStandard ?: Undefined

        return MediaInfo_MediaUriStandard
    }
    //获取当前媒体的uriString
    fun GET_Media_UriString(): String {
        val MediaInfo_MediaUriString = CURRENT_MediaItemPackage?.content_uriString ?: Undefined

        return MediaInfo_MediaUriString
    }
    //获取媒体是视频还是音乐
    fun GET_Media_SPECIFIC_TYPE(): String {
        //尝试获取类型
        val MediaInfo_MediaType = CURRENT_MediaItemPackage?.media_SPECIFIC_MediaType ?: Undefined

        //检查类型是否合法
        if (MediaInfo_MediaType != Video && MediaInfo_MediaType != Audio){
            return Undefined
        }

        return MediaInfo_MediaType
    }
    //获取当前媒体的文件名
    fun GET_Media_FileName(): String {
        val Media_FileName = CURRENT_MediaItemPackage?.file_name ?: Undefined

        return Media_FileName
    }
    //获取当前媒体的艺术家
    fun GET_Media_Artist(): String {
        val MediaInfo_MediaArtist = CURRENT_MediaItemPackage?.media_artist ?: Undefined

        return MediaInfo_MediaArtist
    }
    //获取当前媒体的总时长
    fun GET_Media_Duration(): Long {
        val MediaInfo_MediaDuration = CURRENT_MediaItemPackage?.media_durationMs ?: 0L

        return MediaInfo_MediaDuration
    }
    //获取绝对路径
    fun GET_Media_FilePath(): String {
        val Media_FilePath = CURRENT_MediaItemPackage?.file_path ?: Undefined

        return Media_FilePath
    }
    //获取视频宽高比(返回默认保底值1)
    fun GET_Media_AspectRatio(): Float {
        //获取宽高
        val MediaInfo_Video_Width = CURRENT_MediaItemPackage?.video_videoWidth ?: 1L
        val MediaInfo_Video_Height = CURRENT_MediaItemPackage?.video_videoHeight ?: 1L

        //计算视频宽高比
        val aspectRatio = MediaInfo_Video_Width.toFloat() / MediaInfo_Video_Height.toFloat()

        return aspectRatio
    }



    //清除当前媒体信息
    fun CLEAR_CurrentMediaInfo() {
        //清空当前媒体信息
        CURRENT_MediaItemPackage = null


        //修改可观察标记为空
        _observableMediaItem.value = MediaItemForPlay()
        //修改可观察播放状态为未播放
        _observableIsPlaying.value = false

    }





    //老函数归档
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


//使用uri作为唯一可观察的标签,供外部观察媒体切换事件
    private val _uriString = MutableStateFlow("")
    val uriString: StateFlow<String> = _uriString.asStateFlow()





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