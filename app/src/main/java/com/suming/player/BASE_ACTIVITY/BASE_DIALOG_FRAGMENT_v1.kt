package com.suming.player.BASE_ACTIVITY

import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.suming.player.BASE_TOOL_BUNDLE.BASE_TOOL_BUNDLE
import com.suming.player.FuncionalPack.DeviceInfo
import com.suming.player.FuncionalPack.SettingsCenter
import com.suming.player.R

abstract class BASE_DIALOG_FRAGMENT_v1(
    @LayoutRes private val contentLayoutId: Int,
    private val Fragment_ID: String,

) : DialogFragment() {

    //空字段
    private val Undefined = ""
    //当前系统版本
    private var SDK_INT = Build.VERSION.SDK_INT
    //是否横屏
    private var isLandscape = false
    //是否深色模式
    private var isDarkMode = false
    //是否处于小窗模式
    private var isMultiWindowMode = false
    //根容器
    protected lateinit var outArea: ConstraintLayout
    protected lateinit var rootCard: CardView
    //实现读取状态栏高度
    private fun get_statusBar_Height_Px(): Int {
        consoleLog("get_statusBar_Height_Px:${DeviceInfo.statusBarHeight}")
        if (DeviceInfo.statusBarHeight != -1){
            return DeviceInfo.statusBarHeight
        }

        return 100

    }
    //实现日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "BASE_DIALOG_FRAGMENT_v1：ID：$Fragment_ID: $msg")
        }
    }
    //实现设置读取
    private fun get_setting_useFullScreenFragment(): Boolean {
        return if (isMultiWindowMode) {
            true
        }else{
            SettingsCenter.GET_PRF_UseFullScreenFragment()
        }
    }
    //实现退出锁定逻辑
    var lockPage = false
    fun con_dismiss(){
        if (!lockPage) {
            dismiss()
        }
    }





    override fun onStart() {
        super.onStart()
        //初始化Window
        initWindow()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //设置样式
        setStyle(STYLE_NO_TITLE, R.style.FullScreenDialog)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //初始化根容器
        val root = inflater.inflate(R.layout.base_dialog_fragment_v1_cardview, container, false) as ConstraintLayout
        outArea = root.findViewById(R.id.out_area)
        rootCard = root.findViewById(R.id.main_card)
        rootCard.visibility = View.GONE

        //设置rootCard的宽高信息
        initRootView(rootCard)

        //子类的业务布局塞进rootCard
        inflater.inflate(contentLayoutId, rootCard, true)

        //初始化子类的业务布局的视图
        initViewInstance(root)

        return root
    }




    protected open fun initViewInstance(view: View) {}

    protected open fun initRootView(rootCard: CardView) {
        //检查横竖屏状态
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //检查深色模式
        isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        //检查是否处于小窗模式
        isMultiWindowMode = activity?.isInMultiWindowMode ?: false

        //获取窗口宽高
        val (screen_Height_Px, screen_Width_Px) = if (SDK_INT >= Build.VERSION_CODES.R){
            val windowManager = requireContext().getSystemService(WindowManager::class.java)
            val windowMetrics = windowManager.currentWindowMetrics
            val windowHeight = windowMetrics.bounds.height()
            val windowWidth = windowMetrics.bounds.width()

            Pair(windowHeight, windowWidth)
        }else{

            val rect = Rect()
            requireActivity().window.decorView.getWindowVisibleDisplayFrame(rect)
            val visibleHeight = rect.height()
            val visibleWidth = rect.width()

            Pair(visibleHeight, visibleWidth)
        }

        //执行设置
        if (isLandscape){
            //计算目标宽度
            val targetWidth_Px = if (screen_Height_Px > screen_Width_Px){

                screen_Width_Px
            }else{
                screen_Height_Px
            }


            rootCard.post {
                //设置高度为match parent
                rootCard.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                //设置宽度为目标宽度
                rootCard.layoutParams.width = targetWidth_Px
                //设置顶部padding给状态栏显示
                rootCard.setContentPadding(0, get_statusBar_Height_Px(), get_statusBar_Height_Px(), 0)
                //请求布局
                rootCard.requestLayout()
                //显示rootCard
                rootCard.visibility = View.VISIBLE
            }
        }else{
            //计算目标高度
            val targetHeightPx = if (get_setting_useFullScreenFragment()){
                if (isMultiWindowMode){
                    val BASE_TOOL_BUNDLE = BASE_TOOL_BUNDLE()
                    screen_Height_Px - get_statusBar_Height_Px() - BASE_TOOL_BUNDLE.dpToPx(50f)
                }else{
                    screen_Height_Px - get_statusBar_Height_Px()
                }
            }else{
                (screen_Height_Px * 0.7).toInt()
            }


            rootCard.post {
                //设置宽度为match parent
                rootCard.layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                //设置高度为目标高度
                rootCard.layoutParams.height = targetHeightPx
                //请求布局
                rootCard.requestLayout()
                //显示rootCard
                rootCard.visibility = View.VISIBLE
            }
        }
    }

    protected open fun initWindow() {

        //检查横竖屏状态
        isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        //检查深色模式
        isDarkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES



        //获取window
        val window = dialog?.window ?: return
        //执行window参数设置
        //设置状态栏背景为透明(否则有色块跟随动画飞出)
        window.statusBarColor = Color.TRANSPARENT
        //设置背景压暗幅度
        window.setDimAmount(0f)


        //执行绑定屏幕方向的设置
        if (isLandscape){
            //设置横屏版进场动画
            window.setWindowAnimations(R.style.DialogSlideInOutHorizontal)

            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //监听状态栏变化
                ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, _ -> WindowInsetsCompat.CONSUMED }

                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
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
            //设置竖屏版进场动画
            window.setWindowAnimations(R.style.DialogSlideInOut)

            //执行状态栏设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //监听状态栏变化
                //ViewCompat.setOnApplyWindowInsetsListener(dialog?.window?.decorView ?: return) { view, insets -> WindowInsetsCompat.CONSUMED }
                //显示到挖孔区域
                window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

                //设置状态栏字体颜色
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = !isDarkMode

            }else{
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