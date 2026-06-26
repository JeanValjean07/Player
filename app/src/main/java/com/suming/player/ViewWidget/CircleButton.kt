package com.suming.player.ViewWidget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import com.suming.player.AddonTools.ToolVibrate
import com.suming.player.R
import kotlin.math.max
import kotlin.math.min


class CircleButton @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
):View(context, attrs, defStyleAttr){
    //画笔
    private var mainPaint: Paint? = null
    private var shadowPaint: Paint? = null
    private var iconPaint: Paint? = null
    //尺寸
    private var buttonSize = 0 //按钮直径
    private var shadowOffset = 0
    private var shadowRadius = 0
    //颜色
    private var mainColor = 0
    private var pressedColor = 0
    private var shadowColor = 0
    //状态

    private var iconDrawable: Drawable? = null
    private var iconBitmap: Bitmap? = null
    //区域
    private val circleRect = RectF()
    private val shadowRect = RectF()
    private val iconRect = RectF()
    private var iconPadding = 0
    private var circlePadding = 0



    init {
        init(attrs)
    }
    private fun init(attrs: AttributeSet?) {
        //读取传入属性
        context.withStyledAttributes(attrs, R.styleable.CircleButton) {

            buttonSize = getDimensionPixelSize(R.styleable.CircleButton_buttonSize, dp2px(56))
            shadowOffset = getDimensionPixelSize(R.styleable.CircleButton_shadowOffset, dp2px(4))
            shadowRadius = getDimensionPixelSize(R.styleable.CircleButton_shadowRadius, dp2px(8))
            mainColor = getColor(R.styleable.CircleButton_mainColor, "#FFFFFFFF".toColorInt())
            pressedColor = getColor(R.styleable.CircleButton_pressedColor, darkenColor(mainColor, 0.7f))
            shadowColor = getColor(R.styleable.CircleButton_shadowColor, "#33000000".toColorInt())
            circlePadding = getDimensionPixelSize(R.styleable.CircleButton_circlePadding, dp2px(10))
            iconPadding = getDimensionPixelSize(R.styleable.CircleButton_iconPadding, dp2px(10))

            //获取和绘制图标
            val src = getDrawable(R.styleable.CircleButton_android_src)
            if (src != null) {
                setIconDrawable(src)
            }

        }
        //初始化画笔
        mainPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mainPaint!!.style = Paint.Style.FILL

        shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        shadowPaint!!.style = Paint.Style.FILL
        shadowPaint!!.setColor(shadowColor)

        iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        iconPaint!!.isFilterBitmap = true

        //尺寸写死
        setWillNotDraw(false)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        //强制宽高为固定值,写死
        val size = max(buttonSize, suggestedMinimumWidth)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(newWidthPx: Int, newHeightPx: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidthPx, newHeightPx, oldWidth, oldHeight)
        //强行改为正方形
        if(newWidthPx != newHeightPx){
            if(newWidthPx > newHeightPx){
                setMeasuredDimension(newHeightPx, newHeightPx)
            }
            else{
                setMeasuredDimension(newWidthPx, newWidthPx)
            }
        }
        //引入offset
        val circlePaddingOffset = circlePadding * 0.1f
        val iconPaddingOffset = iconPadding * 0.1f
        //计算圆底方形长度
        val circleEdgeLength = newHeightPx - circlePaddingOffset
        //计算圆底区域的四角坐标
        val circleRectLeft = (newWidthPx - circleEdgeLength) / 2f
        val circleRectTop = (newHeightPx - circleEdgeLength) / 2f
        val circleRectRight = circleRectLeft + circleEdgeLength
        val circleRectBottom = circleRectTop + circleEdgeLength
        //设置圆底区域
        circleRect.set(circleRectLeft, circleRectTop, circleRectRight, circleRectBottom)


        //阴影范围(在circleRect基础上放大一圈)
        shadowRect.set(
            circleRectLeft - shadowOffset,
            circleRectTop - shadowOffset,
            circleRectRight + shadowOffset,
            circleRectBottom + shadowOffset
        )

        //icon范围(独立计算,不限制在circleRect内)
        //计算icon的正方形边长
        val iconEdgeLength = newHeightPx - iconPaddingOffset * 2
        //计算icon区域的四角坐标
        val iconRectLeft = (newWidthPx - iconEdgeLength) / 2f
        val iconRectTop = (newHeightPx - iconEdgeLength) / 2f
        val iconRectRight = iconRectLeft + iconEdgeLength
        val iconRectBottom = iconRectTop + iconEdgeLength
        //设置icon区域
        iconRect.set(
            iconRectLeft,
            iconRectTop,
            iconRectRight,
            iconRectBottom
        )

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //绘制阴影
        drawUniformShadow(canvas)

        //绘制主按钮圆形底
        mainPaint!!.setColor(if (isPressed) pressedColor else mainColor)
        canvas.drawOval(circleRect, mainPaint!!)

        //绘制icon
        if (iconBitmap != null && !iconBitmap!!.isRecycled) {
            val iconSize = iconRect.width().toInt()

            val scaledBitmap = iconBitmap!!.scale(iconSize, iconSize)

            canvas.drawBitmap(scaledBitmap, iconRect.left, iconRect.top, iconPaint)
        }
    }

    //点击事件
    private var pressStartTime: Long = 0L
    private var isPressed = false
    private var isClick = false
    @SuppressLint("NewApi")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {

                ToolVibrate().vibrate(context)

                isPressed = true
                isClick = true
                //记录按压开始时间
                pressStartTime = System.currentTimeMillis()
                //刷新显示按压效果
                invalidate()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isPressed = false
                //刷新显示按压效果
                invalidate()
            }

        }

        if (event.action == MotionEvent.ACTION_UP) {
            //检查手指是否还在按钮视图区域内
            val x = event.x
            val y = event.y
            val isInside = x >= 0 && x <= width && y >= 0 && y <= height

            //记录按压时长
            val pressDuration = System.currentTimeMillis() - pressStartTime

            //长按时抬起也振动一次
            if (pressDuration >= 500) {
                ToolVibrate().vibrate(context)
            }

            //只有手指在区域内才触发点击事件
            if (isInside) {
                performClick()
            }

            // 重置点击状态
            isClick = false
        }
        return true
    }
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }


    //外部控制
    fun setIconDrawable(drawable: Drawable?) {
        this.iconDrawable = drawable
        if (drawable != null) {
            val size = (buttonSize * 0.6f).toInt()
            iconBitmap = drawableToBitmap(drawable, size, size)
        } else {
            iconBitmap = null
        }
        invalidate()
    }
    fun setIconResource(resId: Int) {
        val drawable = ContextCompat.getDrawable(context, resId)
        setIconDrawable(drawable)
    }
    fun setIconTintColor(color: Int) {
        iconPaint?.color = color
        iconPaint?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        invalidate()
    }
    fun setMainColor(color: Int) {
        this.mainColor = color
        this.pressedColor = darkenColor(color, 0.7f)
        invalidate()
    }
    fun setShadow(color: Int, offsetDp: Int) {
        this.shadowColor = color
        this.shadowOffset = dp2px(offsetDp)
        requestLayout()
        invalidate()
    }



    //dp转px
    private fun dp2px(dp: Int): Int {
        return (dp * getContext().getResources().getDisplayMetrics().density + 0.5f).toInt()
    }
    //绘制阴影
    private fun drawUniformShadow(canvas: Canvas) {

        shadowPaint.apply {
            this?.color = shadowColor
            this?.style = Paint.Style.FILL
            this?.maskFilter = BlurMaskFilter(shadowRadius.toFloat(), BlurMaskFilter.Blur.NORMAL)
        }

        //绘制圆形阴影
        val radius = circleRect.width() / 2.3f
        canvas.drawCircle(
            circleRect.centerX(),
            circleRect.centerY(),
            radius,
            shadowPaint!!
        )

        shadowPaint?.maskFilter = null
    }
    //点击时颜色变暗
    private fun darkenColor(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt()
        val g = (Color.green(color) * factor).toInt()
        val b = (Color.blue(color) * factor).toInt()
        return Color.argb(a, min(r, 255), min(g, 255), min(b, 255))
    }

    private fun drawableToBitmap(drawable: Drawable?, width: Int, height: Int): Bitmap? {
        if (drawable == null) return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 回收Bitmap释放内存
        if (iconBitmap != null && !iconBitmap!!.isRecycled) {
            iconBitmap!!.recycle()
            iconBitmap = null
        }
    }

    //日志控制
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "CircleButton: $msg")
        }
    }

}