package com.suming.player.ActivityComponent.PlayerActivity

import android.content.Context
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class SmoothScrollerV2 ( context: Context?): LinearSmoothScroller(context) {


    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_ANY
    }

    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        // 打印目标 View 的当前位置
        val parent = targetView.parent as RecyclerView
        val parentHeight = parent.height
        val targetTop = targetView.top
        val targetBottom = targetView.bottom

        Log.d("SmoothScrollerV2", "=== onTargetFound ===")
        Log.d("SmoothScrollerV2", "targetView.top = $targetTop")
        Log.d("SmoothScrollerV2", "targetView.bottom = $targetBottom")
        Log.d("SmoothScrollerV2", "parent.height = $parentHeight")
        Log.d("SmoothScrollerV2", "当前可见位置: ${(parent.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()}")

        // 调用父类默认实现
        super.onTargetFound(targetView, state, action)
    }


}