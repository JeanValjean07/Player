package com.suming.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.media3.ui.PlayerView

class PlayerCustomPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    // 关键属性：触摸事件是否应该被锁住，强制传递给父视图
    // 默认设置为 true，以便立刻将事件传递出去
    var touchLocked: Boolean = true

    /**
     * 重写 dispatchTouchEvent()
     * 这是触摸事件分发的第一个方法。
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (touchLocked) {
            // 如果触摸被锁定：
            // 1. 将事件传递给父视图，让父视图（playerContainer）来决定是否拦截。
            val handledByParent = super.dispatchTouchEvent(ev)

            // 2. 这里的关键是返回 false，告诉系统：
            //    “这个视图（PlayerView）并没有处理或消耗这个事件。”
            //    注意：虽然调用了 super.dispatchTouchEvent(ev)，但我们最终要保证事件
            //    能被 PlayerContainer 的 onTouchEvent 接收。
            //
            // 实际上，如果父视图处理了事件， super.dispatchTouchEvent(ev) 可能会返回 true。
            // 我们的最终目标是让父视图在 **拦截阶段** 拿到事件，而不是在 PlayerView 消耗后拿到。
            //
            // 为了完全绕过 PlayerView 的内部处理，最好的方法是直接调用父视图的 dispatchTouchEvent。
            // 但是，PlayerView 继承自 FrameLayout（一个 ViewGroup），因此我们尝试直接返回 false，
            // 强制事件冒泡。
            return false

        } else {
            // 如果未被锁定，则走 PlayerView 正常的触摸处理逻辑
            return super.dispatchTouchEvent(ev)
        }
    }

    /**
     * 另一个重写选项：重写 onTouchEvent()
     * onTouchEvent 是视图最终处理事件的地方。
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return if (touchLocked) {
            // 如果锁定，强制返回 false，表示“我没有处理这个事件”。
            // 这使得事件可以冒泡到父视图的 onTouchEvent。
            false
        } else {
            // 否则，执行 PlayerView 默认的触摸处理
            super.onTouchEvent(event)
        }
    }
}