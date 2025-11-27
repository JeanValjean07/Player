package com.suming.player
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.media3.common.util.UnstableApi
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

    private var current_sort_orientation_value = 0

    private var PREFS_CloseFragmentGesture = false



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

        //按钮：退出
        val buttonExit = view.findViewById<ImageButton>(R.id.buttonExit)
        buttonExit.setOnClickListener {
            dismiss()
        }
        //按钮：点击空白区域退出
        val topArea = view.findViewById<View>(R.id.topArea)
        topArea.setOnClickListener {
            dismiss()
        }
        //按钮：锁定页面
        val buttonLock = view.findViewById<ImageButton>(R.id.buttonLock)
        buttonLock.setOnClickListener {
            lockPage = !lockPage
            if (lockPage){
                buttonLock.setImageResource(R.drawable.ic_more_button_lock_on)
            }
            else{
                buttonLock.setImageResource(R.drawable.ic_more_button_lock_off)
            }
        }

        //配置1
        val PREFS_Player = context?.getSharedPreferences("PREFS", Context.MODE_PRIVATE) ?: return
        if (!PREFS_Player.contains("PREFS_CloseFragmentGesture")) {
            PREFS_Player.edit { putBoolean("PREFS_CloseFragmentGesture", false) }
            PREFS_CloseFragmentGesture = false
        } else {
            PREFS_CloseFragmentGesture = PREFS_Player.getBoolean("PREFS_CloseFragmentGesture", false)
        }


        //排序方法预读
        val PREFS = context?.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE) ?: return
        showSortType("")
        showOrientationType("")


        //排序方法修改
        val SortTypeArea = view.findViewById<LinearLayout>(R.id.sort_type_area)
        SortTypeArea.visibility = View.GONE
        val ButtonChangeSortType = view.findViewById<TextView>(R.id.ButtonChangeSort)
        ButtonChangeSortType.setOnClickListener {
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
        val sort_name = view.findViewById<TextView>(R.id.sort_name)
        sort_name.setOnClickListener {
            PREFS.edit { putString("sort_type", "DISPLAY_NAME") }
            showSortType("DISPLAY_NAME")
        }
        val sort_duration = view.findViewById<TextView>(R.id.sort_duration)
        sort_duration.setOnClickListener {
            PREFS.edit { putString("sort_type", "DURATION") }
            showSortType("DURATION")
        }
        val sort_date_added = view.findViewById<TextView>(R.id.sort_date_added)
        sort_date_added.setOnClickListener {
            PREFS.edit { putString("sort_type", "DATE_ADDED") }
            showSortType("DATE_ADDED")
        }
        val sort_resolution = view.findViewById<TextView>(R.id.sort_resolution)
        sort_resolution.setOnClickListener {
            PREFS.edit { putString("sort_type", "RESOLUTION") }
            showSortType("RESOLUTION")
        }
        val ButtonChangeSortOrientation = view.findViewById<TextView>(R.id.ButtonChangeSortOrientation)
        ButtonChangeSortOrientation.setOnClickListener {
            if (current_sort_orientation_value == 0){
                //升序改降序
                PREFS.edit { putString("sort_orientation", "DESC") }
                showOrientationType("DESC")
            }
            else if (current_sort_orientation_value == 1){
                //降序改升序
                PREFS.edit { putString("sort_orientation", "ASC") }
                showOrientationType("ASC")
            }
        }

        //按钮：重读媒体库
        val ButtonReLoadFromMediaStore = view.findViewById<CardView>(R.id.ButtonReLoadFromMediaStore)
        ButtonReLoadFromMediaStore.setOnClickListener {
            val result = bundleOf("KEY" to "ReLoadFromMediaStore")
            setFragmentResult("FROM_FRAGMENT_MediaStore", result)
            dismiss()
        }




        //面板下滑关闭(NestedScrollView)
        if (!PREFS_CloseFragmentGesture){
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
                var down_y = 0f
                var deltaY = 0f
                val RootCard = view.findViewById<CardView>(R.id.mainCard)
                val RootCardOriginY = RootCard.translationY
                val NestedScrollView = view.findViewById<NestedScrollView>(R.id.NestedScrollView)
                var NestedScrollViewAtTop = true
                NestedScrollView.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
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
                            RootCard.translationY = RootCardOriginY + deltaY
                            return@setOnTouchListener true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (deltaY >= 400f){
                                dismiss()
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
                        }
                        MotionEvent.ACTION_MOVE -> {
                            deltaY = event.rawY - down_y
                            deltaX = event.rawX - down_x
                            if (deltaX < 0){
                                return@setOnTouchListener false
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
                                dismiss()
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

    } //onViewCreated END



    //Functions
    fun expand(view: LinearLayout) {
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

    private fun showSortType(type: String){
        val current_sort_type = view?.findViewById<TextView>(R.id.current_sort)
        when(type){
            "DISPLAY_NAME" -> {
                current_sort_type?.text = "已选择：文件名"
            }
            "DURATION" -> {
                current_sort_type?.text = "已选择：时长"
            }
            "DATE_ADDED" -> {
                current_sort_type?.text = "已选择：添加日期"
            }
            "RESOLUTION" -> {
                current_sort_type?.text = "已选择：分辨率"
            }
            "" -> {
                val PREFS = context?.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)
                if (PREFS == null) {
                    current_sort_type?.text = "读取失败"
                    return
                }
                if (PREFS.getString("sort_type", "DISPLAY_NAME") == "DISPLAY_NAME"){
                    current_sort_type?.text = "文件名"
                }else if (PREFS.getString("sort_type", "DISPLAY_NAME") == "DURATION"){
                    current_sort_type?.text = "时长"
                }else if (PREFS.getString("sort_type", "DISPLAY_NAME") == "DATE_ADDED"){
                    current_sort_type?.text = "添加日期"
                }else if (PREFS.getString("sort_type", "DISPLAY_NAME") == "RESOLUTION"){
                    current_sort_type?.text = "分辨率"
                }
            }
        }
    }

    private fun showOrientationType(type: String){
        val current_sort_orientation = view?.findViewById<TextView>(R.id.current_sort_orientation)
        when(type){
            "DESC" -> {
                current_sort_orientation?.text = "已修改为降序"
                current_sort_orientation_value = 1
            }
            "ASC" -> {
                current_sort_orientation?.text = "已修改为升序"
                current_sort_orientation_value = 0
            }
            "" -> {
                val PREFS = context?.getSharedPreferences("PREFS_MediaStore", Context.MODE_PRIVATE)
                if (PREFS == null) {
                    current_sort_orientation?.text = "读取失败"
                    return
                }
                if (PREFS.getString("sort_orientation", "DESC") == "DESC"){
                    current_sort_orientation?.text = "降序"
                    current_sort_orientation_value = 1
                }
                else if (PREFS.getString("sort_orientation", "DESC") == "ASC"){
                    current_sort_orientation?.text = "升序"
                    current_sort_orientation_value = 0
                }
            }
        }
    }




    //自定义退出逻辑
    private fun customDismiss(){
        if (!lockPage) { dismiss() }
    }

}