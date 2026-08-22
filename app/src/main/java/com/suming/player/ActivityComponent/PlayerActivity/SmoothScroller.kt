package com.suming.player.ActivityComponent.PlayerActivity

import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothScroller ( context: Context?, private val itemOffset: Int ): LinearSmoothScroller(context) {

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "SmoothScroller: $msg")
        }
    }

    override fun onStart() {
        super.onStart()
        //consoleLog("onStart: targetPosition = ${targetPosition}")
        //consoleLog("onStart")
    }

    override fun onStop() {
        super.onStop()
        //consoleLog("onStop: mInterimTargetDx=$mInterimTargetDx, mInterimTargetDy=$mInterimTargetDy")
        //consoleLog("onStop")
    }


    override fun getVerticalSnapPreference() = SNAP_TO_START

    override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: Action) {
        super.onSeekTargetStep(dx, dy, state, action)
        //打印调试信息
        /*
        consoleLog(
            "onSeekTargetStep: dx=$dx, dy=$dy, " +
            "mTargetVector=${mTargetVector}, " +
            "mInterimTargetDx=$mInterimTargetDx, mInterimTargetDy=$mInterimTargetDy"
        )
         */
    }

    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        //打印调试信息
        /*
        val parent = targetView.parent as RecyclerView
        consoleLog(
            "onTargetFound: targetView.start=${targetView.left}, " +
                    "targetView.end=${targetView.right}, " +
                    "targetView.width=${targetView.width}, " +
                    "parent.width=${parent.width}"
        )
           */

        //线性插值器
        val animation = LinearInterpolator()


        //计算滚动距离
        val dx = -( calculateDxToMakeVisible(targetView, SNAP_TO_START)  )  + itemOffset
        //consoleLog("onTargetFound 计算移动距离 dx = $dx")


        action.update(dx, 0, 300, animation)


    }





}