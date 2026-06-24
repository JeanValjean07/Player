package com.suming.player.ActivityComponent.MainActivity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.core.content.ContextCompat
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
import com.suming.player.SettingsRequestCenter
import com.suming.player.ViewWidget.CircleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@UnstableApi
@RequiresApi(Build.VERSION_CODES.Q)
class FragmentVideoStoreSetting: DialogFragment() {
    companion object {
        fun newInstance():
                FragmentVideoStoreSetting = FragmentVideoStoreSetting().apply { arguments =
            bundleOf()
        }
    }
    //自动关闭标志位
    private var lockPage = false



    override fun onStart() {
        super.onStart()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){

            //横屏时隐藏状态栏
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                dialog?.window?.decorView?.post { dialog?.window?.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } }
                //三星专用:显示到挖空区域
                dialog?.window?.attributes?.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            } else {
                dialog?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }

            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOutHorizontal)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        }
        else if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            dialog?.window?.setWindowAnimations(R.style.DialogSlideInOut)
            dialog?.window?.setDimAmount(0.1f)
            dialog?.window?.statusBarColor = Color.TRANSPARENT
            dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

            if(context?.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO){
                val decorView: View = dialog?.window?.decorView ?: return
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
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

        register(view)


    }

    private fun init(view: View){
        //
        SortMethodText = view.findViewById(R.id.current_sort)
        SortOrientationText = view.findViewById(R.id.current_sort_orientation)
        //
        lifecycleScope.launch(Dispatchers.Main) {
            //设置卡片高度
            setCardHeight(view)


            //执行其他
            delay(500)
            //监听返回手势
            dialog?.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    Dismiss(false)
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }
        }
    }
    private fun setCardHeight(view: View){
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            //读取屏幕信息
            val screenHeightPx = resources.displayMetrics.heightPixels
            val targetHeightPx = (screenHeightPx * 0.7).toInt()
            val density = resources.displayMetrics.density
            val screenHeightDp = (screenHeightPx / density).toInt()
            //操作主卡片视图
            val mainCard = view.findViewById<CardView>(R.id.main_card)
            mainCard.post {
                if (screenHeightDp < 450){
                    mainCard.layoutParams.height = screenHeightPx
                }else{
                    mainCard.layoutParams.height = targetHeightPx
                }
                mainCard.requestLayout()
            }
        }
    }



    //Main Thread Functions
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
        val topArea = view.findViewById<View>(R.id.topArea)
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
            val result = bundleOf("KEY" to "QueryFromMediaStoreVideo")
            setFragmentResult("FROM_FRAGMENT_VIDEO_MediaStore", result)
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
                //报告并退出
                val result = bundleOf("KEY" to "RenovateAdapter")
                setFragmentResult("FROM_FRAGMENT_VIDEO_MediaStore", result)
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


    //Functions
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

        // 如果目标高度为0，则无需动画
        if (targetHeight <= 0) return
        // 如果当前高度已经是目标高度，则无需动画
        if (view.layoutParams.height == targetHeight) return

        // 初始高度设为0 (为了动画能从0开始)
        view.layoutParams.height = 0
        view.visibility = View.VISIBLE


        val animator = ValueAnimator.ofInt(0, targetHeight)

        // 3. 设置动画更新监听器
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
        var targetSortMethod = ""
        if (sortMethod == "") {
            targetSortMethod = SettingsRequestCenter.get_PREFS_video_sortMethod(requireContext())
        }else{
            targetSortMethod = sortMethod
        }

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
        var targetSortOrientation = ""
        if (sortOrientation == "") {
            //未传入目标时，自己读取
            targetSortOrientation = SettingsRequestCenter.get_PREFS_video_sortOrientation(requireContext())
        }else{
            targetSortOrientation = sortOrientation
        }

        when(targetSortOrientation){
            SettingsRequestCenter.sort_orientation_DESC -> {
                SortOrientationText.text = "降序"
            }
            SettingsRequestCenter.sort_orientation_ASC -> {
                SortOrientationText.text = "升序"
            }
            else -> {
                SortOrientationText.text = "读取时发生错误"
            }
        }

    }
    //自定义退出逻辑
    private fun customDismiss(){
        if (!lockPage) {
            Dismiss()
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ ToolVibrate().vibrate(requireContext()) }

        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()

    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "MainActivity: $msg")
        }
    }

}