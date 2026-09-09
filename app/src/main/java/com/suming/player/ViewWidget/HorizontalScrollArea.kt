package com.suming.player.ViewWidget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.HorizontalScrollView

//描述：基于横滑按钮区：点击空白区域不拦截事件，点击子视图拦截事件



class HorizontalScrollArea @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private var downX = 0f
    private var downY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                downY = ev.y
                //检查点击位置是否有子视图
                if (!isTouchOnChild(ev)) {
                    //点击在空白区域,不拦截事件
                    return false
                }
                //点击在子视图
                return super.onInterceptTouchEvent(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                //只有按下时在子视图上,才会继续处理滚动
                return super.onInterceptTouchEvent(ev)
            }
            else -> return super.onInterceptTouchEvent(ev)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //点击在空白区域,不消费事件
        if (event.action == MotionEvent.ACTION_DOWN && !isTouchOnChild(event)) {
            return false
        }
        return super.onTouchEvent(event)
    }


    //判断触摸点是否在任意子视图上
    private fun isTouchOnChild(ev: MotionEvent): Boolean {
        val x = ev.x
        val y = ev.y

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            //获取子视图在父容器中的位置
            val childLeft = child.left
            val childTop = child.top
            val childRight = child.right
            val childBottom = child.bottom

            //判断触摸点是否在该子视图范围内
            if (x >= childLeft && x <= childRight &&
                y >= childTop && y <= childBottom) {
                return true
            }
        }
        return false
    }
}