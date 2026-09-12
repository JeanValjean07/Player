package com.suming.player.ActivityComponent.MainActivity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.Animations
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.R
import com.suming.player.SettingsCenter
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
@Suppress("unused")
@SuppressLint("NewApi")
class FragmentMusicStoreSetting: DialogFragment() {
    companion object {
        fun newInstance(): FragmentMusicStoreSetting = FragmentMusicStoreSetting().apply { arguments = bundleOf() }
    }

    //空字段
    private val Undefined = ""
    //
    private lateinit var context : Context



    override fun onStart() {
        super.onStart()
        //初始化显示
        initDisplay()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
        //
        context = requireContext()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):View{
        //获得view
        val view = inflater.inflate(R.layout.activity_main_frag_music_mss, container, false)

        //初始化界面
        init(view)

        return view
    }

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        //注册
        register(view)
        registerSortSettings(view)
        registerSettings(view)

        //启动高级效果
        setupScrollContentListener(view)

    }

    private fun init(view: View){
        //初始化常用视图
        SortMethodText = view.findViewById(R.id.current_sort)
        SortOrientationText = view.findViewById(R.id.current_sort_orientation)

        AppBar_Background = view.findViewById(R.id.AppBar_Background)

        //设置卡片高度
        display(view)

    }




    //注册基础控件
    private fun register(view: View){
        lifecycleScope.launch(Dispatchers.Main){
            //按钮：退出
            val ButtonExit = view.findViewById<CircleButton>(R.id.buttonExit)
            ButtonExit.setOnClickListener {
                dismiss()
            }
            //按钮：点击空白区域退出
            val topArea = view.findViewById<View>(R.id.out_area)
            topArea.setOnClickListener {
                dismiss()
            }
            //按钮：锁定页面
            val ButtonLock = view.findViewById<CircleButton>(R.id.buttonLock)
            ButtonLock.setOnClickListener {
                lockPage = !lockPage
                if (lockPage){
                    ButtonLock.setIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_button_lock_on))
                }
                else{
                    ButtonLock.setIconDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.ic_more_button_lock_off))
                }
            }
            //按钮：重读媒体库
            val ButtonReLoadFromMediaStore = view.findViewById<CardView>(R.id.ButtonReLoadFromMediaStore)
            ButtonReLoadFromMediaStore.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                setFragmentResult(FragmentConnector.fragment_media_store_setting_require_mediastore_api_refresh)
                customDismiss()
            }
            //默认页签
            val ButtonTextChangeDefaultTab = view.findViewById<TextView>(R.id.ButtonTextChangeDefaultTab)
            fun setAcquiesceTabText(){
                val AcquiesceTab = SettingsCenter.get_PREFS_AcquiesceTab(requireContext())
                when(AcquiesceTab){
                    SettingsCenter.tab_mark_video -> {
                        ButtonTextChangeDefaultTab.text = "视频"
                    }
                    SettingsCenter.tab_mark_music -> {
                        ButtonTextChangeDefaultTab.text = "音乐"
                    }
                    SettingsCenter.tab_mark_last -> {
                        ButtonTextChangeDefaultTab.text = "上一次的页面"
                    }

                }
            }
            setAcquiesceTabText()
            ButtonTextChangeDefaultTab.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //显示默认页签选择弹窗
                val popupMenu = PopupMenu(requireContext(), it)
                popupMenu.menuInflater.inflate(R.menu.activity_main_popup_default_page, popupMenu.menu)
                popupMenu.show()
                //默认页签选择弹窗点击事件
                popupMenu.setOnMenuItemClickListener { item ->
                    ToolVibrate().vibrate(requireContext())
                    when (item.itemId) {
                        R.id.page_video -> {
                            SettingsCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsCenter.tab_mark_video)

                            setAcquiesceTabText()

                            return@setOnMenuItemClickListener true
                        }
                        R.id.page_music -> {
                            SettingsCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsCenter.tab_mark_music)

                            setAcquiesceTabText()

                            return@setOnMenuItemClickListener true
                        }
                        R.id.page_gallery -> {
                            requireContext().showCustomToast("暂不支持设为陈列架",  3)
                            return@setOnMenuItemClickListener true
                        }
                        R.id.page_last -> {
                            SettingsCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsCenter.tab_mark_last)

                            setAcquiesceTabText()

                            return@setOnMenuItemClickListener true
                        }
                    }
                    false
                }
            }
            //通用设置提示
            val SyncSettingsCard = view.findViewById<LinearLayout>(R.id.SyncSettingsCard)
            SyncSettingsCard.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                requireContext().showCustomToast("这些设置会在音乐库和视频库之间同步",  3)
            }
            //点击顶部区域回顶
            val AppBar_Container = view.findViewById<FrameLayout>(R.id.AppBar_Container)
            AppBar_Container.setOnClickListener {
                if (scrollArea?.canScrollVertically(-1) == true){
                    ToolVibrate().vibrate(requireContext())
                    //滚动区域回顶
                    scrollArea?.stopNestedScroll()
                    scrollArea?.smoothScrollTo(0, 0)
                }else{
                    scrollArea?.stopNestedScroll()
                }
            }



        }
    }
    //注册列表设置项
    private fun registerSortSettings(view: View){
        lifecycleScope.launch(Dispatchers.Main){
            //排序方法读取
            updateSortMethodText()
            updateSortOrientationText()

            //展开排序区域
            val SortOrderArea = view.findViewById<LinearLayout>(R.id.sort_type_area)
            SortOrderArea.visibility = View.GONE
            //排序操作按钮(面板收起时,展开面板, 面板展开时,触发刷新)
            val ButtonChangeSortOrder = view.findViewById<TextView>(R.id.ButtonChangeSort)
            ButtonChangeSortOrder.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //不同状态不同操作
                if (state_expanded){

                    setFragmentResult(FragmentConnector.fragment_media_store_setting_require_recyclerview_refresh)

                    customDismiss()
                }else{
                    //展开面板并替换显示文本
                    ButtonChangeSortOrder.text = "保存并刷新"
                    expand(SortOrderArea)
                }
            }

            //降序和升序
            val ButtonChangeSortOrientation = view.findViewById<TextView>(R.id.ButtonChangeSortOrientation)
            ButtonChangeSortOrientation.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //读取当前升降序配置
                val PREFS_audio_sortOrientation = SettingsCenter.get_PREFS_audio_sortOrientation(requireContext())
                //取反并保存
                if (PREFS_audio_sortOrientation == SettingsCenter.sort_orientation_ASC){
                    SettingsCenter.set_PREFS_audio_sortOrientation(requireContext(), SettingsCenter.sort_orientation_DESC)
                    updateSortOrientationText(SettingsCenter.sort_orientation_DESC)
                }
                else if (PREFS_audio_sortOrientation == SettingsCenter.sort_orientation_DESC){
                    SettingsCenter.set_PREFS_audio_sortOrientation(requireContext(), SettingsCenter.sort_orientation_ASC)
                    updateSortOrientationText(SettingsCenter.sort_orientation_ASC)
                }
            }

            //排序方法
            val sort_method_filename = view.findViewById<TextView>(R.id.sort_name)
            val sort_method_duration = view.findViewById<TextView>(R.id.sort_duration)
            val sort_method_date_added = view.findViewById<TextView>(R.id.sort_date_added)
            val sort_method_file_size = view.findViewById<TextView>(R.id.sort_file_size)
            val sort_method_mime_type = view.findViewById<TextView>(R.id.sort_mime_type)
            sort_method_filename.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //设置排序方法
                SettingsCenter.set_PREFS_audio_sortMethod(requireContext(), SettingsCenter.sort_method_filename)
                updateSortMethodText(SettingsCenter.sort_method_filename)
            }
            sort_method_duration.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //设置排序方法
                SettingsCenter.set_PREFS_audio_sortMethod(requireContext(), SettingsCenter.sort_method_duration)
                updateSortMethodText(SettingsCenter.sort_method_duration)
            }
            sort_method_date_added.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //设置排序方法
                SettingsCenter.set_PREFS_audio_sortMethod(requireContext(), SettingsCenter.sort_method_date_added)
                updateSortMethodText(SettingsCenter.sort_method_date_added)
            }
            sort_method_file_size.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //设置排序方法
                SettingsCenter.set_PREFS_audio_sortMethod(requireContext(), SettingsCenter.sort_method_file_size)
                updateSortMethodText(SettingsCenter.sort_method_file_size)
            }
            sort_method_mime_type.setOnClickListener {
                ToolVibrate().vibrate(requireContext())
                //设置排序方法
                SettingsCenter.set_PREFS_audio_sortMethod(requireContext(), SettingsCenter.sort_method_mime_type)
                updateSortMethodText(SettingsCenter.sort_method_mime_type)
            }
        }
    }
    //注册基本设置项
    private fun registerSettings(view: View){
        lifecycleScope.launch(Dispatchers.Main){
            //检查文件有效性
            val switch_EnableFileExistCheck = view.findViewById<SwitchCompat>(R.id.switch_EnableFileExistCheck)
            switch_EnableFileExistCheck.isChecked = SettingsCenter.get_PREFS_EnableFileExistCheck(context)
            switch_EnableFileExistCheck.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(requireContext())
                SettingsCenter.set_PREFS_EnableFileExistCheck(requireContext(), isChecked)
            }
            //每次启动都读取
            val switch_QueryNewVideoOnStart = view.findViewById<SwitchCompat>(R.id.switch_QueryNewVideoOnStart)
            switch_QueryNewVideoOnStart.isChecked = SettingsCenter.get_PREFS_QueryNewMediaOnStart(context)
            switch_QueryNewVideoOnStart.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(requireContext())
                SettingsCenter.set_PREFS_QueryNewMediaOnStart(requireContext(), isChecked)
            }
            //自动弹出播放页
            val switch_startFullPage = view.findViewById<SwitchCompat>(R.id.SC_startFullPage_whenSwitch)
            switch_startFullPage.isChecked = SettingsCenter.GET_PRF_StartFullPage(context)
            switch_startFullPage.setOnCheckedChangeListener { _, isChecked ->
                ToolVibrate().vibrate(requireContext())
                SettingsCenter.SET_PRF_StartFullPage(context,isChecked)
            }




        }

    }




    //Functions
    //发送Fragment返回值
    private fun setFragmentResult(event: String){
        val result = bundleOf(FragmentConnector.receive_key to event)
        setFragmentResult(FragmentConnector.fragment_request_key_music_store_setting, result)
    }
    //展开动画
    private var state_expanded = false
    private fun expand(view: LinearLayout) {
        if (state_expanded) return
        state_expanded = true
        //设置初始高度为0
        view.measure(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val targetHeight = view.measuredHeight

        if (targetHeight <= 0) return
        if (view.layoutParams.height == targetHeight) return

        view.layoutParams.height = 0
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight)

        animator.addUpdateListener { animation ->
            val animatedValue = animation.animatedValue as Int
            view.layoutParams.height = animatedValue
            view.requestLayout()
        }
        animator.duration = 200

        animator.start()
    }
    //排序方式与方向-显示
    private lateinit var SortMethodText : TextView
    private fun updateSortMethodText(sortMethod: String = ""){
        //读取当前排序方法
        val targetSortMethod = if (sortMethod == "") {
            SettingsCenter.get_PREFS_audio_sortMethod(requireContext())
        }else{
            sortMethod
        }
        //上屏显示排序方法
        when(targetSortMethod){
            SettingsCenter.sort_method_filename -> {
                SortMethodText.text = "文件名"
            }
            SettingsCenter.sort_method_duration -> {
                SortMethodText.text = "时长"
            }
            SettingsCenter.sort_method_date_added -> {
                SortMethodText.text = "添加日期"
            }
            SettingsCenter.sort_method_file_size -> {
                SortMethodText.text = "文件大小"
            }
            SettingsCenter.sort_method_mime_type -> {
                SortMethodText.text = "文件格式"
            }
            //未知排序方式
            else -> {
                SortMethodText.text = "未知"
            }
        }
    }
    private lateinit var SortOrientationText : TextView
    private fun updateSortOrientationText(sortOrientation: String = ""){
        //读取当前排序方向
        val targetSortOrientation = if (sortOrientation == "") {
            SettingsCenter.get_PREFS_audio_sortOrientation(requireContext())
        }else{
            sortOrientation
        }
        //上屏显示排序方向
        when(targetSortOrientation){
            SettingsCenter.sort_orientation_DESC -> {
                SortOrientationText.text = "降序"
            }
            SettingsCenter.sort_orientation_ASC -> {
                SortOrientationText.text = "升序"
            }
            //未知排序方向
            else -> {
                SortOrientationText.text = "未知"
            }
        }
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            dismiss()
        }
    }


    //顶栏效果
    private lateinit var AppBar_Background : LinearLayout
    //顶栏效果
    private var isTopBarVisible = false
    private fun topBarEffect_Fade_In(){
        if (!isTopBarVisible) return
        isTopBarVisible = false

        AppBar_Background.animate()
            .alpha(0f)
            .setDuration(Animations.topBar_Effect_Square_Duration)
            .withEndAction { AppBar_Background.visibility = View.GONE }
            .start()

    }
    private fun topBarEffect_Fade_Out(){
        if (isTopBarVisible) return
        isTopBarVisible = true

        AppBar_Background.visibility = View.VISIBLE
        AppBar_Background.alpha = 0f
        AppBar_Background.animate()
            .alpha(1f)
            .setDuration(Animations.topBar_Effect_Square_Duration)
            .start()
    }
    //滚动区域监听
    private var scrollArea: NestedScrollView? = null
    private fun setupScrollContentListener(view: View){
        if (scrollArea == null){
            scrollArea = view.findViewById(R.id.NestedScrollView)
        }
        scrollArea?.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            //未在顶部时隐藏顶部栏文字区
            if (scrollY == 0){
                topBarEffect_Fade_In()
            }
            else{
                topBarEffect_Fade_Out()
            }
        }
    }
    //模糊效果
    private fun startAddonEffect(view: View){
        //安卓12以前返回
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        AppBar_Background = view.findViewById(R.id.AppBar_Background)


        val blurEffect = RenderEffect.createBlurEffect(15f, 15f, Shader.TileMode.CLAMP);


    }


    //设置面板细节
    private fun display(view: View){
        //获取当前屏幕方向
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //操作主卡片视图
        val mainCard = view.findViewById<CardView>(R.id.main_card)
        //读取屏幕信息
        val screenHeightPx = resources.displayMetrics.heightPixels
        val screenWidthPx = resources.displayMetrics.widthPixels
        val density = resources.displayMetrics.density

        //读取是否启用全屏Fragment
        val useFullScreenFragment = SettingsCenter.GET_PRF_UseFullScreenFragment(requireContext())

        //执行设置
        if (isLandscape){
            //计算目标宽度
            val targetScreenWidthPx = (screenWidthPx * 0.4).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()
            //post执行设置
            mainCard.post {
                if (targetScreenHeightDp < 50){
                    mainCard.layoutParams.width = screenWidthPx
                }else{
                    mainCard.layoutParams.width = targetScreenWidthPx
                }
                //把高度改为match parent
                mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                mainCard.setContentPadding(0, DeviceInfo.statusBarHeight, 0, 0)

                mainCard.requestLayout()
            }
        }else{
            //计算目标高度
            val targetHeightPx = if (useFullScreenFragment){
                screenHeightPx - 2 * DeviceInfo.statusBarHeight
            }else{
                (screenHeightPx * 0.7).toInt()
            }
            //post执行设置
            mainCard.post {

                mainCard.layoutParams.height = targetHeightPx

                mainCard.requestLayout()
            }
        }
    }
    @Suppress("DEPRECATION")
    private fun initDisplay(){
        //获取window
        val window = dialog?.window ?: return
        //检查横竖屏状态
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //检查深色模式
        val isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        //执行通用设置
        //设置状态栏背景为透明(否则有色块跟随动画飞出)
        window.statusBarColor = Color.TRANSPARENT
        //设置背景压暗幅度
        window.setDimAmount(0f)

        //执行绑定屏幕方向的设置
        if (isLandscape){
            //横屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOutHorizontal)


            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, _ -> WindowInsetsCompat.CONSUMED }

                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }else{
            //竖屏

            //设置进场动画
            window.setWindowAnimations(R.style.DialogSlideInOut)



            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //高版本

                //监听状态栏变化
                //ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
                //低版本

                //恢复默认行为
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                if (isDarkMode){
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //or
                            )
                }else{
                    //覆盖本次设置
                    window.decorView.systemUiVisibility = (
                            //隐藏状态栏
                            //View.SYSTEM_UI_FLAG_FULLSCREEN or
                            //设置状态栏划出行为
                            //View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY //or
                            //将内容显示到状态栏下方
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                                    //设置状态栏字体颜色
                                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                            )
                }
            }

        }
    }



}