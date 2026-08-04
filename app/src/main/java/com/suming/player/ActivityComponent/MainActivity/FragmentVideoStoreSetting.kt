package com.suming.player.ActivityComponent.MainActivity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import com.suming.player.R
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.AddonTools.showCustomToast
import com.suming.player.FuncionalPack.FragmentConnector
import com.suming.player.SettingsRequestCenter
import com.suming.player.ViewWidget.CircleButton

@UnstableApi
@SuppressLint("NewApi")
class FragmentVideoStoreSetting: DialogFragment() {
    companion object {
        fun newInstance():
                FragmentVideoStoreSetting = FragmentVideoStoreSetting().apply { arguments =
            bundleOf()
        }
    }



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
    ): View = inflater.inflate(R.layout.activity_main_frag_video_mss, container, false)
    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //初始化
        init(view)

        //注册控件
        register(view)

    }

    private fun init(view: View){
        //初始化常用视图
        SortMethodText = view.findViewById(R.id.current_sort)
        SortOrientationText = view.findViewById(R.id.current_sort_orientation)
        //设置显示重组
        display(view)
    }




    //Main Thread Functions
    //控件注册
    private fun register(view: View){
        //开关实例初始化
        val switch_EnableFileExistCheck = view.findViewById<SwitchCompat>(R.id.switch_EnableFileExistCheck)
        val switch_QueryNewVideoOnStart = view.findViewById<SwitchCompat>(R.id.switch_QueryNewVideoOnStart)
        //开关置位
        switch_EnableFileExistCheck.isChecked = SettingsRequestCenter.get_PREFS_EnableFileExistCheck( requireContext())
        switch_QueryNewVideoOnStart.isChecked = SettingsRequestCenter.get_PREFS_QueryNewMediaOnStart( requireContext())
        //开关点击事件
        switch_EnableFileExistCheck.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_EnableFileExistCheck(requireContext(), isChecked)
        }
        switch_QueryNewVideoOnStart.setOnCheckedChangeListener { _, isChecked ->
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_QueryNewMediaOnStart(requireContext(), isChecked)
        }


        //按钮：退出
        val ButtonExit = view.findViewById<CircleButton>(R.id.buttonExit)
        ButtonExit.setOnClickListener {
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.out_area)
        topArea.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
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
        updateSortMethodText("")
        updateSortOrientationText("")


        //展开排序区域
        val SortOrderArea = view.findViewById<LinearLayout>(R.id.sort_type_area)
        SortOrderArea.visibility = View.GONE
        //排序操作按钮(面板收起时,展开面板, 面板展开时,触发刷新)
        val ButtonChangeSortOrder = view.findViewById<TextView>(R.id.ButtonChangeSort)
        ButtonChangeSortOrder.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //
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
            val PREFS_video_sortOrientation = SettingsRequestCenter.get_PREFS_video_sortOrientation(requireContext())
            //取反并保存
            if (PREFS_video_sortOrientation == SettingsRequestCenter.sort_orientation_ASC){
                SettingsRequestCenter.set_PREFS_video_sortOrientation(requireContext(), SettingsRequestCenter.sort_orientation_DESC)
                updateSortOrientationText(SettingsRequestCenter.sort_orientation_DESC)
            }else if (PREFS_video_sortOrientation == SettingsRequestCenter.sort_orientation_DESC){
                SettingsRequestCenter.set_PREFS_video_sortOrientation(requireContext(), SettingsRequestCenter.sort_orientation_ASC)
                updateSortOrientationText(SettingsRequestCenter.sort_orientation_ASC)
            }
        }
        //排序方法选择区
        val sort_method_filename = view.findViewById<TextView>(R.id.sort_method_filename)
        val sort_method_duration = view.findViewById<TextView>(R.id.sort_method_duration)
        val sort_method_date_added = view.findViewById<TextView>(R.id.sort_method_date_added)
        val sort_method_file_size = view.findViewById<TextView>(R.id.sort_method_file_size)
        val sort_method_mime_type = view.findViewById<TextView>(R.id.sort_method_mime_type)
        sort_method_filename.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            //设置排序方法
            SettingsRequestCenter.set_PREFS_video_sortMethod(requireContext(), SettingsRequestCenter.sort_method_filename)
            updateSortMethodText(SettingsRequestCenter.sort_method_filename)
        }
        sort_method_duration.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_video_sortMethod(requireContext(), SettingsRequestCenter.sort_method_duration)
            updateSortMethodText(SettingsRequestCenter.sort_method_duration)
        }
        sort_method_date_added.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_video_sortMethod(requireContext(), SettingsRequestCenter.sort_method_date_added)
            updateSortMethodText(SettingsRequestCenter.sort_method_date_added)
        }
        sort_method_file_size.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_video_sortMethod(requireContext(), SettingsRequestCenter.sort_method_file_size)
            updateSortMethodText(SettingsRequestCenter.sort_method_file_size)
        }
        sort_method_mime_type.setOnClickListener {
            ToolVibrate().vibrate(requireContext())
            SettingsRequestCenter.set_PREFS_video_sortMethod(requireContext(), SettingsRequestCenter.sort_method_mime_type)
            updateSortMethodText(SettingsRequestCenter.sort_method_mime_type)
        }


    }
    //设置面板显示细节
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
        setFragmentResult(FragmentConnector.fragment_request_key_video_store_setting, result)
    }
    //展开动画
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
    private var state_expanded = false
    //文本显示
    private lateinit var SortMethodText : TextView
    private fun updateSortMethodText(sortMethod: String = ""){
        //读取当前排序方法
        val targetSortMethod = if (sortMethod == "") {
            SettingsRequestCenter.get_PREFS_video_sortMethod(requireContext())
        }else{
            sortMethod
        }
        //上屏显示排序方法
        when(targetSortMethod){
            SettingsRequestCenter.sort_method_filename -> {
                SortMethodText.text = "文件名"
            }
            SettingsRequestCenter.sort_method_duration -> {
                SortMethodText.text = "时长"
            }
            SettingsRequestCenter.sort_method_date_added -> {
                SortMethodText.text = "添加日期"
            }
            SettingsRequestCenter.sort_method_file_size -> {
                SortMethodText.text = "文件大小"
            }
            SettingsRequestCenter.sort_method_mime_type -> {
                SortMethodText.text = "文件格式"
            }
            else -> {
                SortMethodText.text = "读取时发生错误"
            }
        }
    }
    private lateinit var SortOrientationText : TextView
    private fun updateSortOrientationText(sortOrientation: String = ""){
        //读取当前排序方向
        val targetSortOrientation = if (sortOrientation == "") {
            SettingsRequestCenter.get_PREFS_video_sortOrientation(requireContext())
        }else{
            sortOrientation
        }
        //上屏显示排序方向
        when(targetSortOrientation){
            SettingsRequestCenter.sort_orientation_DESC -> {
                SortOrientationText.text = "降序"
            }
            SettingsRequestCenter.sort_orientation_ASC -> {
                SortOrientationText.text = "升序"
            }
            else -> {
                SortOrientationText.text = "未知"
            }
        }
    }
    //获取状态栏高度
    private fun getStatusBarHeightFromView(view: View): Int {
        val rect = Rect()
        view.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }
    //自定义退出逻辑
    private var lockPage = false
    private fun customDismiss(){
        if (!lockPage) {
            dismiss()
        }
    }
    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "FragmentVideoStoreSetting-视频库设置面板: $msg")
        }
    }

    //存档函数
    //监听返回手势(dialog fragment)
    private fun setupBackInvokeCallBackListener(){
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }
    }

}