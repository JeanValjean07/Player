package com.suming.player
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.cardview.widget.CardView
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlin.math.abs

@UnstableApi
class MainActivityFragmentMediaStoreSettings: DialogFragment() {
    //静态
    companion object {
        fun newInstance():
                MainActivityFragmentMediaStoreSettings = MainActivityFragmentMediaStoreSettings().apply { arguments = bundleOf(  ) }
    }
    //自动关闭标志位
    private var lockPage = false
    //开关
    private lateinit var switch_EnableFileExistCheck: SwitchCompat
    //常规设置项
    private lateinit var PREFS_MediaStore: SharedPreferences
    private var PREFS_EnableFileExistCheck: Boolean = false
    private var PREFS_CloseFragmentGesture: Boolean = false
    //排序设置项
    private var PREFS_SortType: String = "info_title"
    private var PREFS_SortOrientation: String = "DESC"


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
    ): View = inflater.inflate(R.layout.activity_main_fragment_media_store_settings, container, false)

    @SuppressLint("UseGetLayoutInflater", "InflateParams", "SetTextI18n", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        //开关实例初始化
        switch_EnableFileExistCheck = view.findViewById(R.id.switch_EnableFileExistCheck)
        //读取设置
        PREFS_MediaStore = context?.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)!!
        if (!PREFS_MediaStore.contains("PREFS_EnableFileExistCheck")) {
            PREFS_MediaStore.edit { putBoolean("PREFS_EnableFileExistCheck", false) }
            PREFS_EnableFileExistCheck = false
        } else {
            PREFS_EnableFileExistCheck = PREFS_MediaStore.getBoolean("PREFS_EnableFileExistCheck", false)
        }
        //开关置位
        switch_EnableFileExistCheck.isChecked = PREFS_EnableFileExistCheck
        //开关点击事件
        switch_EnableFileExistCheck.setOnCheckedChangeListener { _, isChecked ->
            vibrate()
            PREFS_MediaStore.edit { putBoolean("PREFS_EnableFileExistCheck", isChecked) }
        }


        //按钮：退出
        val ButtonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        ButtonExit.setOnClickListener {
            vibrate()
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            vibrate()
            dismiss()
        }
        //按钮：锁定页面
        val ButtonLock = view.findViewById<ImageButton>(R.id.buttonLock)
        ButtonLock.setOnClickListener {
            vibrate()
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
            vibrate()
            val result = bundleOf("KEY" to "ReLoadFromMediaStore")
            setFragmentResult("FROM_FRAGMENT_MediaStore", result)
            customDismiss()
        }


        //排序方法预读
        setAndShowSortType("")
        setAndShowOrientationType("")


        //排序区域
        val SortTypeArea = view.findViewById<LinearLayout>(R.id.sort_type_area)
        SortTypeArea.visibility = View.GONE
        val ButtonChangeSortType = view.findViewById<TextView>(R.id.ButtonChangeSort)
        ButtonChangeSortType.setOnClickListener {
            vibrate()
            if (ButtonChangeSortType.text == "更改"){
                ButtonChangeSortType.text = "立即刷新"
                expand(SortTypeArea)
            }
            else if(ButtonChangeSortType.text == "立即刷新"){
                val result = bundleOf("KEY" to "Refresh Now")
                setFragmentResult("FROM_FRAGMENT_MediaStore", result)
                dismiss()
            }
        }
        //排序方法
        val SortType_info_title = view.findViewById<TextView>(R.id.sort_name)
        val SortType_info_duration = view.findViewById<TextView>(R.id.sort_duration)
        val SortType_info_date_added = view.findViewById<TextView>(R.id.sort_date_added)
        val SortType_info_file_size = view.findViewById<TextView>(R.id.sort_file_size)
        val SortType_info_mime_type = view.findViewById<TextView>(R.id.sort_mime_type)
        SortType_info_title.setOnClickListener {
            vibrate()
            PREFS_MediaStore.edit { putString("PREFS_SortType", "info_title") }
            setAndShowSortType("info_title")
        }
        SortType_info_duration.setOnClickListener {
            vibrate()
            PREFS_MediaStore.edit { putString("PREFS_SortType", "info_duration") }
            setAndShowSortType("info_duration")
        }
        SortType_info_date_added.setOnClickListener {
            vibrate()
            PREFS_MediaStore.edit { putString("PREFS_SortType", "info_date_added") }
            setAndShowSortType("info_date_added")
        }
        SortType_info_file_size.setOnClickListener {
            vibrate()
            PREFS_MediaStore.edit { putString("PREFS_SortType", "info_file_size") }
            setAndShowSortType("info_file_size")
        }
        SortType_info_mime_type.setOnClickListener {
            vibrate()
            PREFS_MediaStore.edit { putString("PREFS_SortType", "info_mime_type") }
            setAndShowSortType("info_mime_type")
        }
        //降序和升序
        val ButtonChangeSortOrientation = view.findViewById<TextView>(R.id.ButtonChangeSortOrientation)
        ButtonChangeSortOrientation.setOnClickListener {
            vibrate()
            //升序改降序
            if (PREFS_SortOrientation == "ASC"){
                PREFS_MediaStore.edit { putString("PREFS_SortOrientation", "DESC") }
                PREFS_SortOrientation = "DESC"
                setAndShowOrientationType("DESC")
            }
            //降序改升序
            else if (PREFS_SortOrientation == "DESC"){
                PREFS_MediaStore.edit { putString("PREFS_SortOrientation", "ASC") }
                PREFS_SortOrientation = "ASC"
                setAndShowOrientationType("ASC")
            }
        }



        //面板下滑关闭(NestedScrollView)
        if (!PREFS_CloseFragmentGesture){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                var deltaY_ReachPadding = false
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginY = RootCard.translationY
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                var NestedScrollViewAtTop = true
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            deltaY_ReachPadding = false
                            if (NestedScrollView.scrollY != 0){
                                NestedScrollViewAtTop = false
                                return@setOnTouchListener false
                            }else{
                                NestedScrollViewAtTop = true
                                down_y = event.rawY
                            }
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (!NestedScrollViewAtTop){
                                return@setOnTouchListener false
                            }
                            deltaY = event.rawY - down_y
                            if (deltaY < 0){
                                return@setOnTouchListener false
                            }
                            if (deltaY >= 400f){
                                if (!deltaY_ReachPadding){
                                    deltaY_ReachPadding = true
                                    vibrate()
                                }
                            }
                            RootCard.translationY = RootCardOriginY + deltaY
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (deltaY >= 400f){
                                Dismiss(false)
                            }else{
                                RootCard.animate()
                                    .translationY(0f)
                                    .setInterpolator(DecelerateInterpolator(1f))
                                    .duration = 300
                            }

                        }
                    }
                    return@setOnTouchListener false
                }
            }
            else if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                var down_y = 0f
                var deltaY = 0f
                var down_x = 0f
                var deltaX = 0f
                var deltaX_ReachPadding = false
                var Y_move_ensure = false
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginX = RootCard.translationX
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            down_x = event.rawX
                            down_y = event.rawY
                            Y_move_ensure = false
                            deltaX_ReachPadding = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            deltaY = event.rawY - down_y
                            deltaX = event.rawX - down_x
                            if (deltaX < 0){
                                return@setOnTouchListener false
                            }
                            if (deltaX >= 200f){
                                if (!deltaX_ReachPadding){
                                    deltaX_ReachPadding = true
                                    vibrate()
                                }
                            }
                            if (Y_move_ensure){
                                return@setOnTouchListener false
                            }
                            if (abs(deltaY) > abs(deltaX)){
                                Y_move_ensure = true
                                return@setOnTouchListener false
                            }
                            RootCard.translationX = RootCardOriginX + deltaX
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (Y_move_ensure){
                                return@setOnTouchListener false
                            }
                            if (deltaX >= 200f){
                                Dismiss(false)
                            }else{
                                RootCard.animate()
                                    .translationX(0f)
                                    .setInterpolator(DecelerateInterpolator(1f))
                                    .duration = 300
                            }
                        }
                    }
                    return@setOnTouchListener false
                }
            }
        }
        //监听返回手势(DialogFragment)
        dialog?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                Dismiss(false)
                return@setOnKeyListener true
            }
            return@setOnKeyListener false
        }

    //onViewCreated END
    }


    //Functions
    //展开动画
    private fun expand(view: LinearLayout) {
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
    //文本显示
    private fun setAndShowSortType(type: String){
        val current_sort_type = view?.findViewById<TextView>(R.id.current_sort)
        when(type){
            "info_title" -> {
                current_sort_type?.text = "已选择：文件名"
                PREFS_SortType = "info_title"
            }
            "info_duration" -> {
                current_sort_type?.text = "已选择：时长"
                PREFS_SortType = "info_duration"
            }
            "info_date_added" -> {
                current_sort_type?.text = "已选择：添加日期"
                PREFS_SortType = "info_date_added"
            }
            "info_file_size" -> {
                current_sort_type?.text = "已选择：文件大小"
                PREFS_SortType = "info_file_size"
            }
            "info_mime_type" -> {
                current_sort_type?.text = "已选择：文件格式"
                PREFS_SortType = "info_mime_type"
            }
            "" -> {
                if (PREFS_MediaStore.contains("PREFS_SortType")){
                    if (PREFS_MediaStore.getString("PREFS_SortType", "info_title") == "info_title"){
                        current_sort_type?.text = "文件名"
                        PREFS_SortType = "info_title"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_SortType", "info_title") == "info_duration"){
                        current_sort_type?.text = "时长"
                        PREFS_SortType = "info_duration"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_SortType", "info_title") == "info_date_added"){
                        current_sort_type?.text = "添加日期"
                        PREFS_SortType = "info_date_added"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_SortType", "info_title") == "info_file_size"){
                        current_sort_type?.text = "文件大小"
                        PREFS_SortType = "info_file_size"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_SortType", "info_title") == "info_mime_type"){
                        current_sort_type?.text = "文件格式"
                        PREFS_SortType = "info_mime_type"
                    }
                    else {
                        PREFS_MediaStore.edit { putString("PREFS_SortType", "info_title") }
                        current_sort_type?.text = "文件名"
                        PREFS_SortType = "info_title"
                    }
                }
                else{
                    PREFS_MediaStore.edit { putString("PREFS_SortType", "info_title") }
                    current_sort_type?.text = "文件名"
                    PREFS_SortType = "info_title"
                }
            }
        }
    }
    private fun setAndShowOrientationType(type_DESC_or_ASC: String){
        val current_sort_orientation = view?.findViewById<TextView>(R.id.current_sort_orientation)
        when(type_DESC_or_ASC){
            "DESC" -> {
                current_sort_orientation?.text = "已修改为降序"
                PREFS_SortOrientation = "DESC"
            }
            "ASC" -> {
                current_sort_orientation?.text = "已修改为升序"
                PREFS_SortOrientation = "ASC"
            }
            "" -> {
                if (PREFS_MediaStore.contains("PREFS_SortOrientation")){
                    if (PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") == "DESC"){
                        current_sort_orientation?.text = "降序"
                        PREFS_SortOrientation = "DESC"
                    }
                    else if (PREFS_MediaStore.getString("PREFS_SortOrientation", "DESC") == "ASC"){
                        current_sort_orientation?.text = "升序"
                        PREFS_SortOrientation = "ASC"
                    }
                    else {
                        PREFS_MediaStore.edit { putString("PREFS_SortOrientation", "DESC") }
                        current_sort_orientation?.text = "降序"
                        PREFS_SortOrientation = "DESC"
                    }
                }
                else{
                    PREFS_MediaStore.edit { putString("PREFS_SortOrientation", "DESC") }
                    current_sort_orientation?.text = "降序"
                    PREFS_SortOrientation = "DESC"
                }
            }
        }
    }
    //震动控制
    @Suppress("DEPRECATION")
    private fun Context.vibrator(): Vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    private fun vibrate() {
        val vib = requireContext().vibrator()
        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        vib.vibrate(effect)
    }
    //自定义退出逻辑
    private fun customDismiss(){
        if (!lockPage) {
            Dismiss()
        }
    }
    private fun Dismiss(flag_need_vibrate: Boolean = true){
        if (flag_need_vibrate){ vibrate() }
        val result = bundleOf("KEY" to "Dismiss")
        setFragmentResult("FROM_FRAGMENT_MORE_BUTTON", result)
        dismiss()

    }

}