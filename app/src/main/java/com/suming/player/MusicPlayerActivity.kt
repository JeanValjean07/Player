package com.suming.player

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.suming.player.ActivityComponent.MusicPlayerActivity.MusicPlayerViewModel
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.DataPack.DataBaseMediaSingleSetting.MediaItemRepo
import com.suming.player.FuncPack_ListManager.ListManagerFragment
import com.suming.player.FuncPack_ListManager.ListManagerHelper
import com.suming.player.FuncionalPack.ActivityResultConnector
import com.suming.player.FuncionalPack.ArtworkCapturer
import com.suming.player.FuncionalPack.ArtworkFrameManager
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.FuncionalPack.IntentRepo
import com.suming.player.FuncionalPack.MediaInfoRetriever
import com.suming.player.FuncionalPack.MediaType
import com.suming.player.FuncionalPack.PlayerInfoCenter
import com.suming.player.FuncionalPack.PlayerListener
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.getValue
import kotlin.math.hypot
import kotlin.math.pow

@OptIn(UnstableApi::class)
@Suppress("NewApi","/unused")
class MusicPlayerActivity : AppCompatActivity() {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MusicPlayerActivity: $msg")
        }
    }
    //ctx
    private val context = this@MusicPlayerActivity
    //连接到viewModel
    private val viewModel: MusicPlayerViewModel by viewModels()

    //播放器引用
    private var player: ExoPlayer ?= null
    //空字段
    private val Undefined = ""
    //播放器监听器
    private val PlayerStateListener = object : Player.Listener {
        @SuppressLint("SwitchIntDef")
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {

                }
                Player.STATE_ENDED -> {

                }
                Player.STATE_IDLE -> {

                }
            }
        }
        //播放状态变更
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            //播放状态变更
            onPlayingStateChanged()
        }

        //媒体项变更(clearItem时会收到null)
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            //consoleLog("onMediaItemTransition: $mediaItem, $reason")
            //媒体项变更
            if (mediaItem == null){
                //媒体项被清除
                onMediaItemCleared()
            }else{
                //媒体项变更
                onMediaItemChanged()
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)

            showCustomToast("播放错误: ${error.message}", 3)
        }
    }
    private var state_PlayerListenerAdded: Boolean = false
    //MediaInfoRetriever
    private val MediaInfoRetriever: MediaInfoRetriever = MediaInfoRetriever()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //显示初始化
        init_display()
        //
        init()


        mainBusiness(savedInstanceState)



        //
        startExoPlayerIdleObserver()

        //注册控件
        register()

        registerSeekBar()

        registerPlayPauseButton()


    }

    override fun onDestroy() {
        super.onDestroy()

        //停止UI端操作
        stopSeekBarSync()
        stopVideoTimeSync()

        //关闭播放器状态监听+丢弃引用
        removeExoPlayerListener()
        player = null
        cache_player_ID = 0L
    }

    override fun finish() {
        super.finish()
        //使用收起动画
        @Suppress("DEPRECATION")
        overridePendingTransition(
                R.anim.slide_just_appear,
                R.anim.slide_out_vertical
            )
    }

    private fun init_display(){
        enableEdgeToEdge()
        setContentView(R.layout.activity_music_player)
        //初始化视图
        fun init_view(){
            seekbar = findViewById(R.id.seekBar)

            time_stamp_current = findViewById(R.id.time_stamp_current)
            time_stamp_duration = findViewById(R.id.time_stamp_duration)

            media_artwork = findViewById(R.id.media_artwork)

            media_title = findViewById(R.id.media_title)
            media_artist = findViewById(R.id.media_artist)
            level_controllers_info = findViewById(R.id.level_controllers_info)

            Button_Play_or_Pause = findViewById(R.id.Button_Play_or_Pause)

        }
        init_view()


    }
    private fun init(){

        //
        volumeDetect()
    }

    private fun mainBusiness(savedInstanceState: Bundle?){

        //获取原始链接
        val URI_U_O = detectOriginalInfo_fromIntent(intent)
        //将URI缓存为字符串
        val URI_S_O = URI_U_O.toString()


        //获取正在播放信息
        val ongoing_URI = PlayerSingleton.GET_STE_currentMediaItem_Uri().second
        val ongoing_MediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()

        //设置媒体项决策程序 savedInstanceState == null 仅在首次启动时决定是否播放
        if (savedInstanceState == null){
            //
            if (URI_S_O == Undefined && ongoing_URI == Uri.EMPTY ){
                //分支描述:未传入播放链接,也没有正在播放的项,弹窗主动输入(彩蛋分支)

                //弹窗主动输入播放链接
                queryManualInputUri()

            }else{
                if (URI_S_O == Undefined){
                    //分支描述:未传入原始链接,检查正在播放的项

                    //检查有没有正在播放的项
                    if (ongoing_URI == Uri.EMPTY){
                        //没有正在播放的项
                        queryManualInputUri()
                    }else{
                        //有正在播放的项
                        if (ongoing_MediaType == MediaType.Audio){
                            //正在播放的是视频,直接绑定
                            connectToCurrentMedia()
                        }else{
                            finish()
                        }
                    }
                }else{
                    //分支-传入了原始链接

                    //检查新项是否与当前项相同
                    if (URI_S_O != ongoing_URI.toString()){
                        //传入链接,但与当前播放项不同,播放新项

                        //发起播放新项
                        startPlayNewMedia(URI_U_O)

                    }else{
                        //传入链接,但与当前播放项相同,直接绑定,但是要先判断是不是视频
                        //consoleLog("传入链接,但与当前播放项相同,直接绑定,但是要先判断是不是视频")
                        if (ongoing_MediaType == MediaType.Audio){
                            //正在播放的是视频,直接绑定
                            connectToCurrentMedia()
                        }else{
                            finish()
                        }
                    }
                }
            }
        }else{
            //
            if (ongoing_URI != Uri.EMPTY){
                connectToCurrentMedia()
            }
        }

    }

    //设置新媒体项
    private suspend fun setNewMediaItem(URI_U_FP: Uri): Boolean{
        if (state_setting_media) return false
        state_setting_media = true
        consoleLog("setNewMediaItem")

        //缓存URI为字符串
        val URI_S_FP = URI_U_FP.toString()

        //确保已启动播放器
        withContext(Dispatchers.Main){ connectToExoPlayer() }


        //确认设置新媒体项
        val result = PlayerSingleton.setMediaItem(URI_U_FP, Undefined,true)
        when(result) {
            ActivityResultConnector.OBRTV_Engine_SetItemSuccess -> {
                //设置成功
                //consoleLog("setNewMediaItem: 设置成功")

                state_setting_media = false

                //返回成功信息
                return true
            }
            ActivityResultConnector.OBRTV_Engine_AlreadyPlayingTargetItem -> {
                //设置失败-目标项已在播放

                //链接当前播放项
                withContext(Dispatchers.Main){ connectToCurrentMedia() }

                state_setting_media = false
                return false

            }
            ActivityResultConnector.OBRTV_Engine_RetrieveFailed -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
                    if (ongoing) {
                        showCustomToast("媒体解码失败")
                        //链接当前播放项
                        connectToCurrentMedia()
                    } else {
                        finish()
                    }
                }

                state_setting_media = false
                //媒体解码失败,结束设置流程
                return false
            }
            ActivityResultConnector.OBRTV_Engine_TypeNotSupport -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
                    if (ongoing) {
                        showCustomToast("不支持的媒体类型")
                        //链接当前播放项
                        connectToCurrentMedia()
                    } else {
                        finish()
                    }
                }

                state_setting_media = false
                //不支持的媒体类型,结束设置流程
                return false
            }
            ActivityResultConnector.OBRTV_Engine_SoFrequent -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
                    if (ongoing) {
                        showCustomToast("设置过于频繁")
                        //链接当前播放项
                        connectToCurrentMedia()
                    } else {
                        finish()
                    }
                }

                state_setting_media = false
                //设置过于频繁,结束设置流程
                return false
            }
            ActivityResultConnector.OBRTV_Engine_Locked -> {
                withContext(Dispatchers.Main){
                    showCustomToast("播放器处于锁定窗口期", 3)
                    finish()

                }

                state_setting_media = false
                return false
            }
            else -> {
                withContext(Dispatchers.Main){
                    //检查当前有没有在播放的项(仅在未播放时显示错误遮罩,在播放时只弹出toast提示)
                    val (ongoing, _) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
                    if (ongoing) {
                        showCustomToast("未知错误")
                        //链接当前播放项
                        connectToCurrentMedia()
                    }else{
                        finish()
                    }
                }

                state_setting_media = false
                //未知错误,结束设置流程
                return false
            }
        }

    }

    private var state_setting_media = false
    //手动输入链接并发起播放
    @SuppressLint("InflateParams")
    private fun queryManualInputUri(){
        val dialog = Dialog(this).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_uri, null)
        dialog.setContentView(dialogView)
        val title: TextView = dialogView.findViewById(R.id.dialog_title)
        val Description: TextView = dialogView.findViewById(R.id.dialog_description)
        val EditText: EditText = dialogView.findViewById(R.id.dialog_input)
        val ButtonEnsure: Button = dialogView.findViewById(R.id.dialog_button_ensure)
        val ButtonCancel: Button = dialogView.findViewById(R.id.dialog_button_cancel)
        var isUriValid = false
        //修改提示文本
        title.text = "未能获取到媒体链接"
        Description.text = "您可以手动输入链接"
        val Editable = Editable.Factory.getInstance().newEditable("content://media/external/video/media/")
        EditText.text = Editable
        ButtonEnsure.text = "确定"
        ButtonCancel.text = "取消并退出"
        //设置点击事件
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        ButtonEnsure.setOnClickListener {
            val userInput = EditText.text.toString()
            if (userInput.isEmpty()){
                showCustomToast("未输入内容", 3)
            }else{
                if (MediaInfoRetriever.isUriStringValid(this, userInput)){
                    isUriValid = true
                    dialog.dismiss()
                    startPlayNewMedia(userInput.toUri())
                }else{
                    showCustomToast("链接无效", 3)
                }
            }
        }
        ButtonCancel.setOnClickListener {
            dialog.dismiss()
            finish()
        }
        dialog.show()
        //接管返回操作
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            if (!isUriValid){ finish() }
        }
        //自动弹出键盘
        CoroutineScope(Dispatchers.Main).launch {
            delay(300)
            EditText.setSelection(Editable.length)
            EditText.requestFocus()
            imm.showSoftInput(EditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }
    //开启播放新媒体项
    private fun startPlayNewMedia(URI_UP: Uri){
        //设置新媒体项
        lifecycleScope.launch(Dispatchers.IO){
            setNewMediaItem(URI_UP)
        }
    }

    //提取原始URI信息
    private fun detectOriginalInfo_fromIntent(intent: Intent): Uri {
        //获取原始链接中的信息
        val Intent_URI = IntentCompat.getParcelableExtra(intent, IntentRepo.URI,  Uri::class.java) ?: Uri.EMPTY


        return Intent_URI
    }




    //注册基本控件
    private fun register(){
        //注册控制按钮
        lifecycleScope.launch(Dispatchers.Main) {
            //退出按钮
            val ButtonExit = findViewById<CircleButton>(R.id.TopBarArea_ButtonExit)
            ButtonExit.setOnClickListener {
                finish()
            }
            //提示卡点击时关闭
            val noticeCard = findViewById<CardView>(R.id.noticeCapsule)
            noticeCard.setOnClickListener {
                ToolVibrate().vibrate(context)

                noticeCard.visibility = View.GONE
            }

            //下一曲
            val Button_Next = findViewById<ImageButton>(R.id.Button_Next)
            Button_Next.setOnClickListener {
                ToolVibrate().vibrate(context)

                //通知切换下一曲
                ListManagerHelper.MediaSessionCall_switchNextMedia()
            }

            //上一曲
            val Button_Prev = findViewById<ImageButton>(R.id.Button_Prev)
            Button_Prev.setOnClickListener {
                ToolVibrate().vibrate(context)

                //通知切换上一曲
                ListManagerHelper.MediaSessionCall_switchPreviousMedia()
            }

            //启动播放列表按钮
            val CommonButton_play_list = findViewById<CircleButton>(R.id.CommonButton_play_list)
            CommonButton_play_list.setOnClickListener {

                startPlayListFragment()
            }

        }
    }



    //播放状态变更回调
    private fun onPlayingStateChanged(){
        //获取当前播放状态
        val isPlaying = player?.isPlaying ?: false


        updatePlayPauseButton()

        if (isPlaying){
            //开启被动控制
            startSeekBarSync()
            startVideoTimeSync()

        }else{
            //停止被动控制
            stopSeekBarSync()
            stopVideoTimeSync()

        }

    }
    //媒体项被清除
    private fun onMediaItemCleared(){
        //停止被动控制
        stopSeekBarSync()
        stopVideoTimeSync()
        //清除当前专辑图
        clearMediaArtWork()

        //清除当前信息
        if (PlayerSingleton.state_real_clear){
            clearMediaTitleArtist()
        }else{
            showMediaTitleArtistLoading()
        }


        //清除时间信息
        clearTimeStamp()

        //SEEK_BAR 重置
        resetSeekBar()

        //刷新播放/暂停按钮
        updatePlayPauseButton()

    }
    //连接到当前媒体
    private fun connectToCurrentMedia(){
        //更新音乐时长
        updateMediaDuration()
        //开始刷新时间
        startVideoTimeSync()
        //刷新媒体封面图
        updateMediaArtwork()
        //刷新媒体标题和作者
        updateMediaTitleArtist()
        //刷新播放/暂停按钮
        updatePlayPauseButton()

        //开启被动控制
        startSeekBarSync()
        startVideoTimeSync()

    }
    //媒体项变更
    private fun onMediaItemChanged(){

        //非音频时主动退出页面
        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        if (mediaType == MediaType.Video){

            Alert_switchToVideoPage()

        }

        //获取专属数据
        if (mediaType == MediaType.Audio){


        }

        //更新音乐时长
        updateMediaDuration()
        //开始刷新时间
        startVideoTimeSync()
        //刷新媒体封面图
        updateMediaArtwork()
        //刷新媒体标题和作者
        updateMediaTitleArtist()
        //刷新播放/暂停按钮
        updatePlayPauseButton()

        //开启被动控制
        startSeekBarSync()
        startVideoTimeSync()

    }


    //连接ExoPlayer(可发起启动)
    private var cache_player_ID = 0L
    private fun connectToExoPlayer(){
        //确保播放器已启动并获得引用
        player = PlayerSingleton.init_player_get_ref()
        //记入缓存
        cache_player_ID = PlayerSingleton.cache_player_instance_id
        //添加播放器事件监听
        startExoPlayerListener()
    }
    private fun startExoPlayerListener(){
        if (state_PlayerListenerAdded) return
        state_PlayerListenerAdded = true
        //consoleLog("startExoPlayerListener")
        player?.addListener(PlayerStateListener)

    }
    private fun removeExoPlayerListener(){
        //consoleLog("removeExoPlayerListener")
        player?.removeListener(PlayerStateListener)
    }
    //播放器ID观察器
    private fun startExoPlayerIdleObserver(){
        lifecycleScope.launch(Dispatchers.Main){
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                PlayerInfoCenter.observableIsIdle.collect { state ->
                    when(state){
                        -1L -> {
                            //播放器从未启动过,不操作

                        }
                        0L -> {
                            //播放器空闲事件
                            onPlayEngineIdle()
                        }
                        else -> {
                            if (state == cache_player_ID){
                                //播放器实例未变更,无操作

                            }else{
                                //播放器重启事件
                                onPlayEnginRestart(state)

                            }
                        }

                    }
                }
            }
        }
    }
    //播放器进入空闲状态
    private fun onPlayEngineIdle(){
        //consoleLog("onPlayEngineIdle")
        //进行收尾工作(移除监听器,移除引用)
        removeExoPlayerListener()
        player = null
        //清除播放项
        onMediaItemCleared()

    }
    //播放器重新上线
    private fun onPlayEnginRestart(new_ID:Long){
        //consoleLog("onPlayEnginRestart: $new_ID")

        //移除一次旧的监听器(保底)
        removeExoPlayerListener()

        //重新拿取引用
        player = PlayerSingleton.get_player_ref()
        if (player == null){
            //consoleLog("onPlayEnginRestart: 未拿到播放器引用")
            return
        }else{
            //consoleLog("onPlayEnginRestart: 成功拿到播放器引用")

            //主动检查正在播放的项
            val (ongoing,ongoing_URI) = PlayerSingleton.GET_STE_currentMediaItem_Uri()
            //consoleLog("onPlayEnginRestart: 正在播放项:ongoing:${ongoing}, ongoing_URI:$ongoing_URI")
            if (ongoing){
                connectToCurrentMedia()
            }
        }
        //记入缓存
        cache_player_ID = new_ID

        //重新添加监听器(将state_PlayerListenerAdded置于false以强制添加)
        state_PlayerListenerAdded = false
        startExoPlayerListener()
    }


    //提示切换到视频页面
    private var switchToVideoPage_alerted = false //一个生命周期只提示一次
    private fun Alert_switchToVideoPage(){
        //一个生命周期只提示一次
        if (switchToVideoPage_alerted) return
        switchToVideoPage_alerted = true

        AlertDialog.Builder(this)
            .setTitle("切换到视频页面?")
            .setMessage("当前播放项已变更为视频")
            .setPositiveButton("确认") { dialog, _ ->
                ToolVibrate().vibrate(context)

                switchToVideoPage()

                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                ToolVibrate().vibrate(context)

                dialog.dismiss()
            }
            .setCancelable(true)
            .show()

    }
    private fun switchToVideoPage(){
        //检查使用的页面类型
        val playPageType = SettingsCenter.GET_PRF_PlayPageType(context)
        when{
            (playPageType == SettingsCenter.PlayPageType_Oro || playPageType == SettingsCenter.PlayPageType_Neo) -> {
                //构建intent
                val intent = Intent(this, PlayerActivityNeo::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                    .putExtra(IntentRepo.SOURCE, 3)

                //构建可选参数
                val options = ActivityOptionsCompat.makeCustomAnimation(
                        this,
                        R.anim.slide_in_vertical,
                        R.anim.slide_dont_move
                    )

                //启动活动
                startActivity(intent, options.toBundle())

            }

        }

        //关闭音乐页面
        finish()
    }


    //启动播放列表面板
    private fun startPlayListFragment(){

        ListManagerFragment.newInstance().show(supportFragmentManager, FragmentConnector.fragment_tag_play_list)
    }

    //播放/暂停音乐
    private fun pausePlay(){
        player?.pause()
    }
    private fun continuePlay(){
        player?.play()
    }


    //播放/暂停按钮
    private lateinit var Button_Play_or_Pause : ImageView
    private fun updatePlayPauseButton(){
        //获取当前播放状态
        val isPlaying = player?.isPlaying ?: false

        if (isPlaying){
            Button_Play_or_Pause.setImageResource(R.drawable.ic_main_controller_pause)
        }else{
            Button_Play_or_Pause.setImageResource( R.drawable.ic_main_controller_play)
        }
    }
    private fun registerPlayPauseButton(){
        Button_Play_or_Pause.setOnClickListener {
            //获取当前播放状态
            val isPlaying = player?.isPlaying ?: false

            if (isPlaying){
                player?.pause()
            }else{
                player?.play()
            }
        }
    }

    //标题和艺术家
    private lateinit var media_title : TextView
    private lateinit var media_artist : TextView
    private lateinit var level_controllers_info : LinearLayout
    private fun updateMediaTitleArtist(){
        val title = PlayerInfoCenter.GET_Media_Title()
        val artist = PlayerInfoCenter.GET_Media_Artist()
        val file_name = PlayerInfoCenter.GET_Media_FileName().substringBeforeLast(".")

        val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
        var PRF_UseFileNameAsTitle = if (mediaType == MediaType.Audio){
            SettingsCenter.GET_PRF_Audio_UseFileNameAsTitle(context)
        }else{
            true
        }


        //显示内容
        fun updateContent(){
            media_title.text = if (PRF_UseFileNameAsTitle) file_name else title
            media_artist.text = artist
        }
        updateContent()


        //设置点击事件
        media_title.setOnClickListener {
            ToolVibrate().vibrate(context)
            //切换跑马灯状态
            if (it.isSelected){
                media_title.isSelected = false
            }else{
                media_title.isSelected = true
            }
        }
        media_artist.setOnClickListener {
            ToolVibrate().vibrate(context)
            //切换跑马灯状态
            if (it.isSelected){
                media_title.isSelected = false
            }else{
                media_title.isSelected = true
            }
        }
        level_controllers_info.setOnClickListener {
            ToolVibrate().vibrate(context)

            //切换使用的标题
            if (mediaType == MediaType.Audio){
                PRF_UseFileNameAsTitle = !PRF_UseFileNameAsTitle

                //显示提示
                if (PRF_UseFileNameAsTitle){
                    notice("已切换为使用文件名",3000)
                }else{
                    notice("已切换为使用元数据标题",3000)
                }
            }else{
                PRF_UseFileNameAsTitle = true
            }

            updateContent()

        }

    }
    @SuppressLint("SetTextI18n")
    private fun clearMediaTitleArtist(){
        media_title.text = "未在播放"
        media_artist.text = "未在播放"
    }
    private fun showMediaTitleArtistLoading(){
        media_title.text = "加载中"
        media_artist.text = "加载中"
    }


    //艺术图 ARTWORK
    private lateinit var media_artwork : ImageView
    private fun updateMediaArtwork(){
        //检查是否显示专辑图片(不显示也要注册点击事件)
        var dont_show_album = SettingsCenter.GET_PRF_Audio_DontShowAlbumFrame(context)
        //获取缓存URI
        val URI = PlayerInfoCenter.GET_Media_URI_S_FP()

        //注册点击事件
        registerMediaArtworkClickEvent(URI)

        //检查是否显示专辑图片
        if (dont_show_album) return
        //检查独立设置是否允许显示专辑图片
        lifecycleScope.launch (Dispatchers.IO){

            //先检查数据库中有没有该项,没有时新建
            if (!MediaItemRepo.get(context).checkExist(URI)){
                MediaItemRepo.get(context).createMediaItem(URI)
                //将此项设置设为true(默认显示，注意这里数据库跟prefs项的意思是反的)
                MediaItemRepo.get(context).update_PREFS_ShowAlbumFrame(URI,true)
                dont_show_album = false
            }else{
                //检查是否显示专辑图片
                dont_show_album = !MediaItemRepo.get(context).get_PREFS_ShowAlbumFrame(URI)
            }
            //检查是否显示专辑图片
            if (dont_show_album) {

                withContext(Dispatchers.Main){
                    clearMediaArtWork()
                }

                return@launch
            }

            //执行显示
            withContext(Dispatchers.Main){

                //获取当前媒体类型(可以把视频当音乐播放)
                val mediaType = PlayerInfoCenter.GET_Media_SPECIFIC_TYPE()
                if (mediaType != MediaType.Audio) {
                    //使用默认
                    clearMediaArtWork()

                    return@withContext
                }
                //获取媒体ID
                val media_NUM_ID = PlayerInfoCenter.GET_Media_NUM_ID()
                //获取封面图片
                val artwork = ArtworkFrameManager.GET_ArtworkFrame_Audio_Album(context,media_NUM_ID)
                if (artwork == null) {
                    lifecycleScope.launch (Dispatchers.IO){
                        //获取当前媒体 URI
                        val URI_U = PlayerInfoCenter.GET_Media_URI_S_FP().toUri()

                        //截取大图
                        val bitmap = ArtworkCapturer.captureAlbumInMusic(context, URI_U ,needCompress = false)
                        if (bitmap != null){
                            //显示
                            withContext(Dispatchers.Main){ media_artwork.setImageBitmap(bitmap) }
                        }else{
                            //使用默认
                            withContext(Dispatchers.Main){ clearMediaArtWork() }

                            return@launch
                        }

                    }

                    return@withContext
                }
                //图片上屏
                media_artwork.setImageBitmap(artwork)


            }


        }


    }
    private fun clearMediaArtWork(){
        //清除当前专辑图
        media_artwork.setImageBitmap(null)
    }
    //为专辑图设置点击事件
    @SuppressLint("ClickableViewAccessibility")
    private fun registerMediaArtworkClickEvent(URI_S:String){
        //获取设置
        val useVolumeGesture = SettingsCenter.GET_PRF_Audio_UseArtworkGesture(context)
        //为专辑图设置点击事件
        val media_artwork_click_layer = findViewById<View>(R.id.media_artwork_click_layer)
        media_artwork_click_layer.post{
            //初始化点击变量
            var touchArea = 0  //点击区域: 1 = left, 2 = right, 3 = middle
            var finger1x = 0f  //手指1的横轴坐标
            //未整理
            var scrollDistance = 0
            var touchCenterDistance = 0f
            var touchState_need_exit = false
            var touchState_need_exit_vibrated = false
            var touchState_scroll_vibrated = false
            var longPress = false

            //获取点击层宽度
            val artwork_width_pixels = media_artwork_click_layer.width

            //注册gestureDetector
            val gestureDetectorPlayArea = GestureDetector(this,object:GestureDetector.SimpleOnGestureListener() {
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    //控制播放状态
                    if (player?.isPlaying == true) {
                        pausePlay()

                        notice("暂停播放", 1000)
                    }else{

                        continuePlay()
                        notice("继续播放", 1000)

                    }

                    return true
                }
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

                    //触发控件显示变更
                    //changeBackgroundColor()

                    return true
                }
                override fun onLongPress(e: MotionEvent) {
                    ToolVibrate().vibrate(context)

                    //切换专辑单项数据库中的显示专辑封面状态
                    lifecycleScope.launch(Dispatchers.IO) {
                        //取反设置显示专辑封面状态
                        MediaItemRepo.get(context).update_PREFS_ShowAlbumFrame(URI_S,!MediaItemRepo.get(context).get_PREFS_ShowAlbumFrame(URI_S))

                        withContext(Dispatchers.Main){
                            updateMediaArtwork()
                        }
                    }



                    super.onLongPress(e)
                }
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float):Boolean {
                    when(touchArea){
                        1 -> {
                            //原本是调整亮度,但此处不提供此功能
                        }
                        2 -> {
                            //累积滑动距离
                            scrollDistance += distanceY.toInt()
                            //快速下滑紧急静音
                            if (scrollDistance < -150) {
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                            }
                            //普通音量修改
                            if (scrollDistance > volumeChangeGap) {
                                var currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                currentVolume += 1
                                if (currentVolume <= maxVolume) {
                                    if (state_HeadSetInserted) {
                                        if (currentVolume <= (maxVolume * 0.6).toInt()) {
                                            audioManager.setStreamVolume(
                                                AudioManager.STREAM_MUSIC,
                                                currentVolume,
                                                0
                                            )
                                            notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                        } else {
                                            if (!touchState_scroll_vibrated) {
                                                touchState_scroll_vibrated = true
                                                ToolVibrate().vibrate(this@MusicPlayerActivity)
                                            }
                                            notice(
                                                "佩戴耳机时,音量不能超过${(maxVolume * 0.6).toInt()},除非使用音量键调整",
                                                1000
                                            )
                                        }
                                    } else {
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            currentVolume,
                                            0
                                        )
                                        notice("音量 +1 ($currentVolume/$maxVolume)", 1000)
                                    }
                                } else {
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@MusicPlayerActivity)
                                    }
                                    notice("音量已到最高", 1000)
                                }
                            } else if (scrollDistance < -volumeChangeGap) {
                                var currentVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                currentVolume -= 1
                                if (currentVolume >= 0) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        currentVolume,
                                        0
                                    )
                                    notice("音量 -1 ($currentVolume/$maxVolume)", 1000)
                                } else {
                                    if (!touchState_scroll_vibrated) {
                                        touchState_scroll_vibrated = true
                                        ToolVibrate().vibrate(this@MusicPlayerActivity)
                                    }
                                    notice("音量已到最低", 1000)
                                }
                            }
                            //数值越界置位
                            if (scrollDistance > 50 || scrollDistance < -50) {
                                scrollDistance = 0
                            }
                        }
                        3 -> {
                            touchCenterDistance += distanceY
                            if (touchCenterDistance < -viewModel.value_scrollDownExitDistance) {
                                touchState_need_exit = true
                                //振动:仅一次
                                if (!touchState_need_exit_vibrated) {
                                    touchState_need_exit_vibrated = true
                                    ToolVibrate().vibrate(this@MusicPlayerActivity)
                                }
                            }else{
                                touchState_need_exit = false
                            }
                        }
                    }

                    return super.onScroll(e1, e2, distanceX, distanceY)
                }
            })
            //注册基本触摸事件
            media_artwork_click_layer.setOnTouchListener { _, event ->
                when (event.actionMasked){
                    MotionEvent.ACTION_DOWN -> {
                        //重置部分状态
                        touchState_need_exit_vibrated = false
                        touchState_need_exit = false
                        touchState_scroll_vibrated = false

                        //记录手指1的初始横轴位置
                        finger1x = event.x

                        //判断点击区域并记录
                        if (useVolumeGesture){
                            when{
                                finger1x < artwork_width_pixels * 0.2 -> {
                                    touchArea = 2 //映射到2上做音量控制
                                }
                                finger1x > artwork_width_pixels * 0.8 -> {
                                    state_HeadSetInserted = PlayerListener.getState_isHeadsetPlugged(this@MusicPlayerActivity)
                                    touchArea = 2
                                }
                                else -> {
                                    touchArea = 3
                                    touchCenterDistance = 0f
                                }
                            }
                        }else{
                            touchArea = 3
                        }


                        //传递
                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_UP -> {

                        //重置部分状态
                        touchArea = 0
                        //重置部分数值
                        scrollDistance = 0

                        //事件处理:长按
                        longPress = false


                        //事件处理:下滑退出
                        if (touchState_need_exit) finish()


                        //传递
                        gestureDetectorPlayArea.onTouchEvent(event)
                    }
                    MotionEvent.ACTION_MOVE -> {

                        //传递
                        gestureDetectorPlayArea.onTouchEvent(event)
                    }

                }

                onTouchEvent(event)
            }
        }
    }



    //时间戳 TIME_STAMP
    private lateinit var time_stamp_current: TextView
    private lateinit var time_stamp_duration: TextView
    @SuppressLint("SetTextI18n")
    private fun clearTimeStamp(){
        //清除时间信息
        time_stamp_current.text = "00:00"
        time_stamp_duration.text = "00:00"
    }
    //刷新时间总长
    private fun updateMediaDuration(){
        //获取时长
        //val duration = player?.duration ?: return
        val duration = PlayerInfoCenter.GET_Media_Duration()
        if (duration < 0) return

        time_stamp_duration.text = FormatTime_onlyNum(duration)

    }
    //单次刷新时间戳
    private fun timeStampSync_by_seekbar(progress: Int){
        //计算当前progress对应视频进度
        val duration = PlayerInfoCenter.GET_Media_Duration()
        if (duration < 0) return

        val target_time = duration * progress / 1000

        time_stamp_current.text = FormatTime_onlyNum(target_time)
    }
    //Runnable-2:根据视频时间更新时间窗口进度数值
    private val task_timeStampSync_Handler = Handler(Looper.getMainLooper())
    private fun timeStampSync_Core(){
        //获取当前进度
        timeStampSync_cache_currentPosition = player?.currentPosition ?: return

        time_stamp_current.text = FormatTime_onlyNum(timeStampSync_cache_currentPosition)
    }
    private var task_timeStampSync_Runnable = object : Runnable{
        override fun run() {
            timeStampSync_Core()

            task_timeStampSync_Handler.postDelayed(this, 350)
        }
    }
    private var timeStampSync_cache_currentPosition = 0L
    private fun startVideoTimeSync(){
        if (task_timeStampSync_Running) return
        val isPlaying = player?.isPlaying ?: false
        if (!isPlaying) {
            return
        }
        task_timeStampSync_Running = true


        task_timeStampSync_Handler.post(task_timeStampSync_Runnable)
    }
    private fun stopVideoTimeSync(){
        if (!task_timeStampSync_Running) return
        task_timeStampSync_Running = false

        task_timeStampSync_Handler.removeCallbacks(task_timeStampSync_Runnable)
    }
    private var value_timeStamp_updateGapMs = 10L //时间戳刷新间隔
    private var task_timeStampSync_Running = false


    //控件 SEEKBAR
    private lateinit var seekbar : SeekBar
    //注册 SEEK_BAR 控制函数
    private fun registerSeekBar(){
        lifecycleScope.launch(Dispatchers.Main){
            seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser){
                        val onProgressChanged_currentMillis = System.currentTimeMillis()
                        if (onProgressChanged_currentMillis - seekbar_updateTimerStamp_lastMillis < value_timeStamp_updateGapMs) return
                        seekbar_updateTimerStamp_lastMillis = onProgressChanged_currentMillis

                        //刷新显示
                        timeStampSync_by_seekbar(progress)

                    }
                }

                //触摸立即触发
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    //关闭被动控制
                    stopSeekBarSync()
                    stopVideoTimeSync()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    //读取进度条当前位置比例
                    val duration = player?.duration ?: -1L
                    val progress = seekBar?.progress ?: -1
                    if (duration <= 0L || progress < 0) return
                    val seekToPosition = (progress / 1000f * duration).toLong()
                    //consoleLog("duration = $duration, seekToPosition = $seekToPosition")

                    player?.seekTo(seekToPosition)
                    //确保在播放
                    player?.play()

                    //开启被动控制
                    startSeekBarSync()
                    startVideoTimeSync()
                }
            })

            //开启被控
            if (player?.isPlaying == true){
                startSeekBarSync()
            }else{
                syncSeekBarPosition_Core()
            }
        }
    }
    private var seekbar_updateTimerStamp_lastMillis = 0L
    private fun resetSeekBar(){
        seekbar.progress = 1
    }
    //Runnable-1.1:根据视频时间更新 SEEK_BAR 位置 SEEK_BAR 被动刷新
    private val task_syncSeekBarPosition_Handler = Handler(Looper.getMainLooper())
    private fun syncSeekBarPosition_Core(){
        val duration = player?.duration ?: -1L
        val currentPosition = player?.currentPosition ?: -1L
        if (duration == -1L || currentPosition == -1L) return
        //计算视频进度比例
        val progressRatio = currentPosition / duration.toFloat()
        //consoleLog("progressRatio = $progressRatio")
        //设定进度条位置(步进1000)
        seekbar.progress = (progressRatio * 1000).toInt()
    }
    private val task_syncSeekBarPosition_Runnable = object : Runnable {
        override fun run(){

            //
            syncSeekBarPosition_Core()

            //进入下一次循环
            task_syncSeekBarPosition_Handler.postDelayed(this,value_syncSeekBar_runnableGapMs )  //value_syncSeekBar_runnableGapMs
        }
    }
    private fun startSeekBarSync() {
        //consoleLog("startSeekBarSync $num_random")
        //进入锁
        if (task_syncSeekBarPosition_Running) return
        task_syncSeekBarPosition_Running = true

        //发起滚动任务
        task_syncSeekBarPosition_Handler.post(task_syncSeekBarPosition_Runnable)
    }
    private fun stopSeekBarSync() {
        task_syncSeekBarPosition_Running = false
        task_syncSeekBarPosition_Handler.removeCallbacks(task_syncSeekBarPosition_Runnable)
    }
    private var value_syncSeekBar_runnableGapMs = 100L //seekbar位置刷新间隔
    private var task_syncSeekBarPosition_Running = false



    //音量管理与提示
    private fun volumeDetect(){
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeChangeGap = 750/maxVolume
        if (originalVolume == 0 && !viewModel.NOTICED_VolumeIsZero) {
            viewModel.NOTICED_VolumeIsZero = true
            notice("当前未开启声音", 1000)
        }
    }
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }
    private var maxVolume = 0
    private var currentVolume = 0
    private var originalVolume = 0
    private var volumeChangeGap = 1   //音量变化步长
    private var state_HeadSetInserted = false

    //格式化时间戳显示
    @SuppressLint("DefaultLocale")
    private fun FormatTime_onlyNum(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours == 0L){
            String.format("%02d:%02d",  minutes, seconds)
        }else{
            String.format("%02d:%02d:%02d",  hours, minutes, seconds)
        }
    }
    private fun FormatTime_withChar(raw: Long): String {
        val cent  = raw % 1000
        val totalSec = raw / 1000
        val min  = totalSec / 60
        val sec  = totalSec % 60
        return "%02d:%02d.%03d".format(min, sec, cent)
    }
    //显示通知
    private var showNoticeJob: Job? = null
    private var showNoticeJobLong: Job? = null
    private fun showNoticeJob(text: String, duration: Long) {
        showNoticeJob?.cancel()
        showNoticeJob = lifecycleScope.launch {
            val NoticeCardText = findViewById<TextView>(R.id.NoticeCardText)
            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
            NoticeCard.visibility = View.VISIBLE
            NoticeCardText.text = text
            delay(duration)
            NoticeCard.visibility = View.GONE
        }
    }
    private fun showNoticeJobLong(text: String) {
        showNoticeJobLong?.cancel()
        showNoticeJobLong = lifecycleScope.launch {
            val NoticeCardText = findViewById<TextView>(R.id.NoticeCardText)
            val NoticeCard = findViewById<CardView>(R.id.noticeCapsule)
            NoticeCard.visibility = View.VISIBLE
            NoticeCardText.text = text
        }
    }
    private fun notice(text: String, duration: Long) {
        if (duration > 114513){
            showNoticeJobLong(text)
        }else{
            showNoticeJob(text, duration)
        }
    }


}