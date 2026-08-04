package com.suming.player.ActivityComponent.MainActivity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.SettingsRequestCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@UnstableApi
@Suppress("unused")
@SuppressLint("NewApi")
class FragmentMusicStoreSetting: DialogFragment() {
    companion object {
        fun newInstance(): FragmentMusicStoreSetting = FragmentMusicStoreSetting().apply { arguments = bundleOf() }
    }


    //常规设置项
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_EnableFileExistCheck: Boolean = false
    private var PREFS_QueryNewVideoOnStart: Boolean = false

    //排序设置项
    private var PREFS_music_sortOrder: String = "info_title"
    private var PREFS_music_sortOrientation: String = "DESC"





    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { _, _ -> WindowInsetsCompat.CONSUMED }
                //三星专用:显示到挖空区域
                dialog?.window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                @Suppress("DEPRECATION")
                dialog?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOutHorizontal)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION")
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            @Suppress("DEPRECATION")
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                @Suppress("DEPRECATION")
                decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.activity_main_frag_music_mss, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化
        init(view)

        register(view)


    }

    private fun init(view: View){
        //初始化常用视图
        SortMethodText = view.findViewById(R.id.current_sort)
        SortOrientationText = view.findViewById(R.id.current_sort_orientation)

        //设置卡片高度
        display(view)

    }




    //Main Thread Functions
    private fun register(view: View){
        lifecycleScope.launch(Dispatchers.IO){
            //delay(100)

            //读取设置
            PREFS_MediaStore = context?.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)!!
            if (!PREFS_MediaStore.contains("PREFS_EnableFileExistCheck")) {
                PREFS_MediaStore.edit { putBoolean("PREFS_EnableFileExistCheck", false) }
                PREFS_EnableFileExistCheck = false
            } else {
                PREFS_EnableFileExistCheck = PREFS_MediaStore.getBoolean("PREFS_EnableFileExistCheck", false)
            }
            if (!PREFS_MediaStore.contains("PREFS_QueryNewVideoOnStart")) {
                PREFS_MediaStore.edit { putBoolean("PREFS_QueryNewVideoOnStart", false) }
                PREFS_QueryNewVideoOnStart = false
            } else {
                PREFS_QueryNewVideoOnStart = PREFS_MediaStore.getBoolean("PREFS_QueryNewVideoOnStart", false)
            }



            //执行主线程操作
            withContext(Dispatchers.Main){
                //开关实例初始化
                val switch_EnableFileExistCheck = view.findViewById<SwitchCompat>(R.id.switch_EnableFileExistCheck)
                val switch_QueryNewVideoOnStart = view.findViewById<SwitchCompat>(R.id.switch_QueryNewVideoOnStart)
                //开关置位
                switch_EnableFileExistCheck.isChecked = PREFS_EnableFileExistCheck
                switch_QueryNewVideoOnStart.isChecked = PREFS_QueryNewVideoOnStart
                //开关点击事件
                switch_EnableFileExistCheck.setOnCheckedChangeListener { _, isChecked ->
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putBoolean("PREFS_EnableFileExistCheck", isChecked) }
                }
                switch_QueryNewVideoOnStart.setOnCheckedChangeListener { _, isChecked ->
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putBoolean("PREFS_QueryNewVideoOnStart", isChecked) }
                }


                //按钮：退出
                val ButtonExit = view.findViewById<ImageButton>(R.id.buttonExit)
                ButtonExit.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    dismiss()
                }
                //按钮：点击空白区域退出
                val topArea = view.findViewById<View>(R.id.out_area)
                topArea.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    dismiss()
                }
                //按钮：锁定页面
                val ButtonLock = view.findViewById<ImageButton>(R.id.buttonLock)
                ButtonLock.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    lockPage = !lockPage
                    if (lockPage){
                        ButtonLock.setImageResource(R.drawable.ic_more_button_lock_on)
                    }
                    else{
                        ButtonLock.setImageResource(R.drawable.ic_more_button_lock_off)
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
                    val AcquiesceTab = SettingsRequestCenter.get_PREFS_AcquiesceTab(requireContext())
                    when(AcquiesceTab){
                        SettingsRequestCenter.tab_mark_video -> {
                            ButtonTextChangeDefaultTab.text = "视频"
                        }
                        SettingsRequestCenter.tab_mark_music -> {
                            ButtonTextChangeDefaultTab.text = "音乐"
                        }
                        SettingsRequestCenter.tab_mark_last -> {
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
                                SettingsRequestCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsRequestCenter.tab_mark_video)

                                setAcquiesceTabText()

                                return@setOnMenuItemClickListener true
                            }
                            R.id.page_music -> {
                                SettingsRequestCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsRequestCenter.tab_mark_music)

                                setAcquiesceTabText()

                                return@setOnMenuItemClickListener true
                            }
                            R.id.page_gallery -> {
                                requireContext().showCustomToast("暂不支持设为陈列架",  3)
                                return@setOnMenuItemClickListener true
                            }
                            R.id.page_last -> {
                                SettingsRequestCenter.set_PREFS_AcquiesceTab(requireContext(), SettingsRequestCenter.tab_mark_last)

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





                //排序方法预读
                setAndShowSortOrder("")
                setAndShowOrientationType("")


                //展开排序区域
                val SortOrderArea = view.findViewById<LinearLayout>(R.id.sort_type_area)
                SortOrderArea.visibility = View.GONE
                //刷新
                val ButtonChangeSortOrder = view.findViewById<TextView>(R.id.ButtonChangeSort)
                ButtonChangeSortOrder.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    if (ButtonChangeSortOrder.text == "更改"){
                        ButtonChangeSortOrder.text = "保存并刷新"
                        expand(SortOrderArea)
                    }
                    else if(ButtonChangeSortOrder.text == "保存并刷新"){
                        setFragmentResult(FragmentConnector.fragment_media_store_setting_require_recyclerview_refresh)
                        customDismiss()
                    }
                }
                //排序方法
                val SortOrder_info_title = view.findViewById<TextView>(R.id.sort_name)
                val SortOrder_info_duration = view.findViewById<TextView>(R.id.sort_duration)
                val SortOrder_info_date_added = view.findViewById<TextView>(R.id.sort_date_added)
                val SortOrder_info_file_size = view.findViewById<TextView>(R.id.sort_file_size)
                val SortOrder_info_mime_type = view.findViewById<TextView>(R.id.sort_mime_type)
                SortOrder_info_title.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_title") }
                    setAndShowSortOrder("info_title")
                }
                SortOrder_info_duration.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_duration") }
                    setAndShowSortOrder("info_duration")
                }
                SortOrder_info_date_added.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_date_added") }
                    setAndShowSortOrder("info_date_added")
                }
                SortOrder_info_file_size.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_file_size") }
                    setAndShowSortOrder("info_file_size")
                }
                SortOrder_info_mime_type.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_mime_type") }
                    setAndShowSortOrder("info_mime_type")
                }
                //降序和升序
                val ButtonChangeSortOrientation = view.findViewById<TextView>(R.id.ButtonChangeSortOrientation)
                ButtonChangeSortOrientation.setOnClickListener {
                    ToolVibrate().vibrate(requireContext())
                    //升序改降序
                    if (PREFS_music_sortOrientation == "ASC"){
                        PREFS_MediaStore.edit { putString("PREFS_music_sortOrientation", "DESC") }
                        PREFS_music_sortOrientation = "DESC"
                        setAndShowOrientationType("DESC")
                    }
                    //降序改升序
                    else if (PREFS_music_sortOrientation == "DESC"){
                        PREFS_MediaStore.edit { putString("PREFS_music_sortOrientation", "ASC") }
                        PREFS_music_sortOrientation = "ASC"
                        setAndShowOrientationType("ASC")
                    }
                }


            }
        }
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

        if (isLandscape){
            //计算目标宽度
            val targetScreenWidthPx = (screenWidthPx * 0.4).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()

            mainCard.post {
                if (targetScreenHeightDp < 50){
                    mainCard.layoutParams.width = screenWidthPx
                }else{
                    mainCard.layoutParams.width = targetScreenWidthPx
                }
                //把高度改为match parent
                mainCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                val statusBarHeight = getStatusBarHeightFromView(mainCard)
                mainCard.setContentPadding(0, statusBarHeight, 0, 0)

                mainCard.requestLayout()
            }

        }else{
            //计算目标高度
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val targetScreenHeightDp = (screenHeightPx / density).toInt()

            mainCard.post {
                if (targetScreenHeightDp < 450){
                    mainCard.layoutParams.height = screenHeightPx
                }else{
                    mainCard.layoutParams.height = targetHeightPx
                }
                mainCard.requestLayout()
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
    private fun expand(view: LinearLayout) {
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
    private fun setAndShowSortOrder(type: String){

        //
        when(type){
            "info_title" -> {
                SortMethodText.text = "已选择：文件名"
                PREFS_music_sortOrder = "info_title"
            }
            "info_duration" -> {
                SortMethodText.text = "已选择：时长"
                PREFS_music_sortOrder = "info_duration"
            }
            "info_date_added" -> {
                SortMethodText.text = "已选择：添加日期"
                PREFS_music_sortOrder = "info_date_added"
            }
            "info_file_size" -> {
                SortMethodText.text = "已选择：文件大小"
                PREFS_music_sortOrder = "info_file_size"
            }
            "info_mime_type" -> {
                SortMethodText.text = "已选择：文件格式"
                PREFS_music_sortOrder = "info_mime_type"
            }
            "" -> {
                if (PREFS_MediaStore.contains("PREFS_music_sortOrder")){
                    if (PREFS_MediaStore.getString("PREFS_music_sortOrder", "info_title") == "info_title"){
                        SortMethodText.text = "文件名"
                        PREFS_music_sortOrder = "info_title"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_music_sortOrder", "info_title") == "info_duration"){
                        SortMethodText.text = "时长"
                        PREFS_music_sortOrder = "info_duration"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_music_sortOrder", "info_title") == "info_date_added"){
                        SortMethodText.text = "添加日期"
                        PREFS_music_sortOrder = "info_date_added"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_music_sortOrder", "info_title") == "info_file_size"){
                        SortMethodText.text = "文件大小"
                        PREFS_music_sortOrder = "info_file_size"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_music_sortOrder", "info_title") == "info_mime_type"){
                        SortMethodText.text = "文件格式"
                        PREFS_music_sortOrder = "info_mime_type"
                    }
                    else {
                        PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_title") }
                        SortMethodText.text = "文件名"
                        PREFS_music_sortOrder = "info_title"
                    }
                }
                else{
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrder", "info_title") }
                    SortMethodText.text = "文件名"
                    PREFS_music_sortOrder = "info_title"
                }
            }
        }
    }
    private lateinit var SortOrientationText : TextView
    private fun setAndShowOrientationType(type_DESC_or_ASC: String){
        //
        when(type_DESC_or_ASC){
            "DESC" -> {
                SortOrientationText.text = "已修改为降序"
                PREFS_music_sortOrientation = "DESC"
            }
            "ASC" -> {
                SortOrientationText.text = "已修改为升序"
                PREFS_music_sortOrientation = "ASC"
            }
            "" -> {
                if (PREFS_MediaStore.contains("PREFS_music_sortOrientation")){
                    if (PREFS_MediaStore.getString("PREFS_music_sortOrientation", "DESC") == "DESC"){
                        SortOrientationText.text = "降序"
                        PREFS_music_sortOrientation = "DESC"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_music_sortOrientation", "DESC") == "ASC"){
                        SortOrientationText.text = "升序"
                        PREFS_music_sortOrientation = "ASC"
                    }
                    else {
                        PREFS_MediaStore.edit { putString("PREFS_music_sortOrientation", "DESC") }
                        SortOrientationText.text = "降序"
                        PREFS_music_sortOrientation = "DESC"
                    }
                }
                else{
                    PREFS_MediaStore.edit { putString("PREFS_music_sortOrientation", "DESC") }
                    SortOrientationText.text = "降序"
                    PREFS_music_sortOrientation = "DESC"
                }
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
    //获取状态栏高度
    private fun getStatusBarHeightFromView(view: View): Int {
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }

}