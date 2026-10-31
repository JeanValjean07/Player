package com.suming.player.ActivityComponent.OssPage

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.UI_SEG_Compose.ComposeElementBundle
import com.suming.player.R

class OssLicensePage : AppCompatActivity() {


    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", msg)
        }
    }
    //ctx
    private val context = this@OssLicensePage
    //振动
    @Suppress("NewApi")
    private fun vibrate(){
        ToolVibrate.vibrate()
    }
    //ComposeElementBundle
    private lateinit var ComposeElementBundle: ComposeElementBundle
    //深色模式标记
    private var isDarkMode: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //界面配置
        enableEdgeToEdge()
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        //手动设置状态栏字体颜色
        setStatusBarFontColor()

        //初始化颜色模式
        isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        //托管给ComposableRoot
        setContent {
            ComposeRoot()
        }

    }


    @Composable
    fun ComposeRoot() {
        //使用Box作为根布局
        Box(
            modifier = Modifier.fillMaxSize()
        ){
            //初始化组件仓库
            ComposeElementBundle = ComposeElementBundle(isDarkMode)


            //获取状态栏高度
            val statusBarHeight_Px = WindowInsets.statusBars.getTop(LocalDensity.current)
            val statusBarHeight_Dp = with(LocalDensity.current) { statusBarHeight_Px.toDp() }

            //缓存总高度值
            var topAll_Height_Px by remember { mutableIntStateOf(300) }
            val topAll_Height_Dp = with(LocalDensity.current) { topAll_Height_Px.toDp() }


            //许可证列表
            ADVANCED_CONTENT_AREA_CONTENT(
                contentPadding = PaddingValues(top = topAll_Height_Dp),
            )

            //最顶层
            ComposeElementBundle.ADVANCED_TOP_BAR_c_v1_single(
                onHeightMeasured = { height -> topAll_Height_Px = height },
                modifier = Modifier,
                statusBarHeight_Dp = statusBarHeight_Dp,
                brushContent = { ADVANCED_TOP_BAR_Brush(statusBarHeight_Dp, topAll_Height_Dp - statusBarHeight_Dp) },
                content = { ADVANCED_TOP_BAR_CONTENT() },
            )
        }
    }


    //内容区域是否在顶部标记
    private val isContentTop = mutableStateOf(true)
    //标题内容compose
    @Composable
    fun ADVANCED_TOP_BAR_CONTENT(){
        @Composable
        fun content(){
            //标题文本
            Text(
                text = "开放源代码许可",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ComposeElementBundle.ColorPack.primary,
                modifier = Modifier.padding(start = 10.dp)
            )
        }

        val top = isContentTop.value
        val capsuleProgress by animateFloatAsState(
            targetValue = if (top) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "capsuleProgress"
        )

        Box {

            //胶囊标题区
            ComposeElementBundle.CAPSULE_BUTTON_c_v2_avoid_circle(
                text_onClick = { },
                modifier = Modifier.padding(start = 15.dp),
                elevation = if (top) 10.dp else 0.dp,
                backgroundColor = if (isDarkMode) {
                    Color(0xFA181818).copy(alpha = if (top) 1f else 0f)
                } else {
                    Color(0xFFFFFFFF).copy(alpha = if (top) 1f else 0f)
                },
                border = BorderStroke(
                    width = 0.3.dp * capsuleProgress,
                    color = Color.Gray.copy(alpha = 0.3f * capsuleProgress)
                ),
                padding_start = 0.dp,
                padding_end = 15.dp,
                padding_vertical = 0.dp,
                text_content = { content() },
            )

            //返回按钮
            ComposeElementBundle.CIRCLE_BUTTON_c_v1 (
                onClick = {
                    vibrate()
                    finish() },
                backgroundColor = if (isDarkMode){
                    Color(0xFA181818)
                }else{
                    Color(0xFFFFFFFF)
                },
                border = BorderStroke(width = 0.3.dp, color = Color.Gray.copy(alpha = 0.3f)),
                modifier = Modifier.padding(start = 15.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "退出",
                    modifier = Modifier.background(Color.Transparent),
                    tint = ComposeElementBundle.ColorPack.secondary
                )
            }


        }
    }
    @Composable
    fun ADVANCED_TOP_BAR_Brush(statusBarHeight_Dp: Dp, content_Height_Dp: Dp){
        ComposeElementBundle.BRUSH_AREA_TOP_c_v2(
            color = ComposeElementBundle.ColorPack.surface,
            statusBarHeight_Dp = statusBarHeight_Dp,
            content_Height_Dp = content_Height_Dp
        )
    }
    //主要内容区域compose
    @Composable
    fun ADVANCED_CONTENT_AREA_CONTENT(contentPadding: PaddingValues = PaddingValues()){
        //加载构建时自动生成的许可证数据
        val libraries by produceLibraries(R.raw.aboutlibraries)

        val darkTheme = isSystemInDarkTheme()
        MaterialTheme(
            colorScheme = if (darkTheme) {
                ComposeElementBundle.DarkColorScheme
            } else {
                ComposeElementBundle.LightColorScheme
            }
        ) {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        }


    }




    //深浅色模式检测+设置状态栏字体颜色
    fun colorModeDetector(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> true
            Configuration.UI_MODE_NIGHT_YES -> false
            else -> true
        }
    }
    fun setStatusBarFontColor() {
        val insetsController = WindowInsetsControllerCompat(
            window, window.decorView
        )
        insetsController.isAppearanceLightStatusBars = colorModeDetector(this)
    }

}