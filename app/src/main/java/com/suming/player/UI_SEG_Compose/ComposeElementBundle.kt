package com.suming.player.UI_SEG_Compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class ComposeElementBundle (isDarkMode_o: Boolean) {

    //深色模式标记
    private var isDarkMode: Boolean = false
    //颜色配置
    var ColorPack: ColorScheme
    //颜色指定
    val LightColorScheme = lightColorScheme(
        //全局底色
        surface = Color(0xFFFFFFFF),
        //一级和二级文字
        primary = Color(0xFF000000),
        secondary = Color(0xFF313131),
        //活动底色
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFFFFFFFF),
    )
    val DarkColorScheme = darkColorScheme(
        //全局底色
        surface = Color(0xFF000000),
        //一级和二级文字
        primary = Color(0xFFFFFFFF),
        secondary = Color(0xFFF6F6F6),
        //活动底色
        background = Color(0xFF000000),
        onBackground = Color(0xFF212121),
    )



    //初始化
    init {
        //记录深色模式
        isDarkMode = isDarkMode_o
        //初始化颜色配置
        ColorPack = if (isDarkMode) DarkColorScheme else LightColorScheme
    }


    private val CAPSULE_BUTTON_Height_Dp = 40.dp
    private val CIRCLE_BUTTON_Size_Dp = 40.dp

    //圆形悬浮按钮 v1
    @Composable
    fun CIRCLE_BUTTON_c_v1(
        onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = CIRCLE_BUTTON_Size_Dp, backgroundColor: Color = MaterialTheme.colorScheme.primary,
        gradient: Brush? = null, border: BorderStroke? = null, elevation: Dp = 3.dp, enabled: Boolean = true, content: @Composable () -> Unit
    ) {
        val backgroundModifier = when {
            gradient != null -> Modifier.background(gradient)
            else -> Modifier.background(backgroundColor)
        }
        Box(
            modifier = modifier
                .size(size)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.4f),  // 控制阴影颜色
                    ambientColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .then(backgroundModifier)
                .then(if (border != null) Modifier.border(border, CircleShape) else Modifier)
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = Color.Gray
                    )
                ) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }

    //胶囊按钮
    @Composable
    fun CAPSULE_BUTTON_c_v1(
        onClick: () -> Unit, modifier: Modifier = Modifier,height: Dp = CAPSULE_BUTTON_Height_Dp,backgroundColor: Color = ColorPack.onBackground,
        border: BorderStroke = BorderStroke(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.1f)), elevation: Dp = 5.dp, enabled: Boolean = true,
        padding_start: Dp = 15.dp,padding_end: Dp = 15.dp, padding_vertical: Dp = 0.dp, content: @Composable RowScope.() -> Unit
    ){
        val backgroundModifier = Modifier.background(backgroundColor)
        Box(
            modifier = modifier
                .wrapContentWidth()
                .height(height)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.4f),
                    ambientColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .then(backgroundModifier)
                .then(Modifier.border(border, CircleShape))
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = Color.Gray
                    )
                ) { onClick() }
                .padding(start = padding_start, end = padding_end, top = padding_vertical, bottom = padding_vertical)
        ) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }

    @Composable
    fun CAPSULE_BUTTON_c_v2_avoid_circle(
        modifier: Modifier = Modifier,height: Dp = CAPSULE_BUTTON_Height_Dp,backgroundColor: Color = ColorPack.onBackground,
        border: BorderStroke = BorderStroke(width = 0.5.dp, color = Color.Gray.copy(alpha = 0.1f)), elevation: Dp = 5.dp, enabled: Boolean = true,
        padding_start: Dp = 15.dp,padding_end: Dp = 15.dp, padding_vertical: Dp = 0.dp,
        text_content: @Composable RowScope.() -> Unit, text_onClick: () -> Unit,
    ){
        val backgroundModifier = Modifier.background(backgroundColor)
        Box(
            modifier = modifier
                .wrapContentWidth()
                .height(height)
                .shadow(
                    elevation = elevation,
                    shape = CircleShape,
                    clip = false,
                    spotColor = Color.Black.copy(alpha = 0.4f),
                    ambientColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(CircleShape)
                .then(backgroundModifier)
                .then(Modifier.border(border, CircleShape))
                .clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = Color.Gray
                    )
                ){ text_onClick() }
                .padding(start = padding_start, end = padding_end, top = padding_vertical, bottom = padding_vertical)
        ){

            Row(
                modifier = Modifier.fillMaxHeight().padding(start = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = text_content
            )
        }
    }


    //顶部渐变区  v1
    @Composable
    fun BRUSH_AREA_TOP_c_v1(
        modifier: Modifier = Modifier,color: Color = ColorPack.surface,
        statusBarHeight_Dp: Dp = ADVANCED_TOP_BAR_default_statusBar_height_Dp,content_Height_Dp: Dp = ADVANCED_TOP_BAR_content_height_Dp
    ) {
        Column {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(statusBarHeight_Dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.85f ),
                                color.copy(alpha = 0.85f ),
                            ),
                        )
                    )
            )

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(content_Height_Dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.85f ),
                                color.copy(alpha = 0.85f ),
                                color.copy(alpha = 0.85f ),
                                color.copy(alpha = 0.85f ),
                                color.copy(alpha = 0.80f ),
                                color.copy(alpha = 0.0f)
                            ),
                        )
                    )
            )

        }

    }

    //顶部渐变区  v2
    @Composable
    fun BRUSH_AREA_TOP_c_v2(
        modifier: Modifier = Modifier,color: Color = ColorPack.surface,
        statusBarHeight_Dp: Dp = ADVANCED_TOP_BAR_default_statusBar_height_Dp,content_Height_Dp: Dp = ADVANCED_TOP_BAR_content_height_Dp
    ) {
        Column {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(statusBarHeight_Dp + content_Height_Dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.95f ),
                                color.copy(alpha = 0.0f)
                            ),
                        )
                    )
            )

        }

    }




    //ADVANCED_TOP_BAR Family
    private val ADVANCED_TOP_BAR_content_height_Dp = 60.dp  //内容区域高度统一为 60 Dp
    private val ADVANCED_TOP_BAR_default_statusBar_height_Dp = 20.dp  //状态栏高度未传入或有问题时,用 20 Dp保底,宁可往下多一点,至少不被挡住

    //顶栏  内容区域完全空白
    @Composable
    fun ADVANCED_TOP_BAR_c_v1_single(
        onHeightMeasured: (height: Int) -> Unit, modifier: Modifier = Modifier, statusBarHeight_Dp: Dp = ADVANCED_TOP_BAR_default_statusBar_height_Dp,
        containerColor: Color = Color.Transparent,brushContent: @Composable RowScope.() -> Unit = {},content: @Composable RowScope.() -> Unit
    ) {

        //计算区域总高度 Dp
        val total_height_Dp = statusBarHeight_Dp + ADVANCED_TOP_BAR_content_height_Dp

        //
        Surface(
            modifier = modifier
                .fillMaxWidth()
                //.statusBarsPadding()
                .onGloballyPositioned { coordinates ->
                    onHeightMeasured(coordinates.size.height)
                },
            color = containerColor,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height( total_height_Dp )
            ) {
                //渐变区域
                Row {
                    brushContent()
                }

                //
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = statusBarHeight_Dp).height(ADVANCED_TOP_BAR_content_height_Dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }

    @Composable
    fun ADVANCED_TOP_BAR_c_v2_dual(
        onHeightMeasured: (height: Int) -> Unit, modifier: Modifier = Modifier, containerColor: Color = Color.Transparent,
        brushContent: @Composable RowScope.() -> Unit = {},statusBarHeight_Dp: Dp = ADVANCED_TOP_BAR_default_statusBar_height_Dp,
        startContent: @Composable RowScope.() -> Unit = {}, endContent: @Composable RowScope.() -> Unit = {}
    ) {
        //计算区域总高度 Dp
        val total_height_Dp = statusBarHeight_Dp + ADVANCED_TOP_BAR_content_height_Dp


        Surface(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .onGloballyPositioned { onHeightMeasured(it.size.height) },
            color = containerColor,
        ) {
            //渐变区域
            Row {
                brushContent()
            }

            //左右带插槽
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(59.dp)
                    .padding(horizontal = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = startContent
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = endContent
                )
            }
        }
    }

    //ADVANCED_TOP_BAR 配套内容区域
    private val ADVANCED_CONTENT_AREA_top_padding_Dp = ADVANCED_TOP_BAR_content_height_Dp + ADVANCED_TOP_BAR_default_statusBar_height_Dp

    @Composable
    fun ADVANCED_CONTENT_AREA_Unknow_c_v1(
        topPadding_Dp: Dp = ADVANCED_CONTENT_AREA_top_padding_Dp,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding_Dp)
        ) {
            content()
        }
    }

    @Composable
    fun ADVANCED_CONTENT_AREA_LazyColumn_c_v1(
        topPadding_Dp : Dp = ADVANCED_CONTENT_AREA_top_padding_Dp,
        content: LazyListScope.() -> Unit
    ){
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding_Dp),
            content = content
        )
    }

    @Composable
    fun ADVANCED_CONTENT_AREA_NestedScroll_c_v1(
        topPadding_Dp : Dp = ADVANCED_CONTENT_AREA_top_padding_Dp,
        content: @Composable ColumnScope.() -> Unit
    ){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            //顶部填充
            Spacer(Modifier.height(topPadding_Dp))
            //内容
            content()
        }
    }





}