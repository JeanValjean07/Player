package com.suming.player.FuncPack_ListManager

import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs
import kotlin.math.sqrt

class LinearDecelerateSnapHelper(private val context: Context, private val isHorizontal: Boolean = true):SnapHelper() {

    companion object {
        //速度阈值:低于此值视为无滑动意图
        private const val MIN_VELOCITY = 200
        //速度与翻页数的换算系数
        private const val VELOCITY_TO_PAGE_RATIO = 500f
        //最大一次翻页数
        private const val MAX_PAGES_TO_SNAP = 1
        //最小滚动时间Ms
        private const val MIN_SCROLL_TIME = 80
        //最大滚动时间Ms
        private const val MAX_SCROLL_TIME = 800
        //基准速度, 用于计算减速曲线
        private const val BASE_SPEED = 4000f
    }

    //缓存当前容器中心位置
    private var cachedCenterX = 0
    private var cachedCenterY = 0

    //记录最近一次fling的初始速度用于计算减速曲线
    private var lastFlingVelocity = 0




    //根据速度计算目标页码（支持翻多页）
    override fun findTargetSnapPosition(
        layoutManager: RecyclerView.LayoutManager,
        velocityX: Int,
        velocityY: Int
    ): Int {
        consoleLog("findTargetSnapPosition: velocityX=$velocityX, velocityY=$velocityY")

        //获取当前居中的 View 及其位置
        val currentView = findSnapView(layoutManager) ?: run {
            consoleLog("findTargetSnapPosition: currentView == null")
            return RecyclerView.NO_POSITION
        }

        val currentPos = layoutManager.getPosition(currentView)
        if (currentPos == RecyclerView.NO_POSITION) {
            consoleLog("findTargetSnapPosition: currentPos == NO_POSITION")
            return RecyclerView.NO_POSITION
        }

        val itemCount = layoutManager.itemCount
        if (itemCount == 0) {
            return RecyclerView.NO_POSITION
        }

        //判断滚动方向并计算目标位置
        val targetPos = when {
            //水平滚动
            isHorizontal && abs(velocityX) > MIN_VELOCITY -> {
                lastFlingVelocity = velocityX
                calculateTargetPage(currentPos, velocityX, itemCount)
            }
            //垂直滚动
            !isHorizontal && abs(velocityY) > MIN_VELOCITY -> {
                lastFlingVelocity = velocityY
                calculateTargetPage(currentPos, velocityY, itemCount)
            }
            //速度不足 留在当前页
            else -> {
                consoleLog("findTargetSnapPosition: 速度不足，保持当前页")
                currentPos
            }
        }

        consoleLog("findTargetSnapPosition: targetPos=$targetPos")
        return targetPos.coerceIn(0, itemCount - 1)
    }


    //获取最接近容器中心的子 View
    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        consoleLog("findSnapView")

        val childCount = layoutManager.childCount
        if (childCount == 0) {
            return null
        }

        //缓存中心位置
        updateCenterCache(layoutManager)

        var closestView: View? = null
        var minDistance = Int.MAX_VALUE

        for (i in 0 until childCount) {
            val child = layoutManager.getChildAt(i) ?: continue

            val distance = if (isHorizontal) {
                val childCenterX = (child.left + child.right) / 2
                abs(childCenterX - cachedCenterX)
            } else {
                val childCenterY = (child.top + child.bottom) / 2
                abs(childCenterY - cachedCenterY)
            }

            if (distance < minDistance) {
                minDistance = distance
                closestView = child
            }
        }

        consoleLog("findSnapView: closestView=${closestView?.let { layoutManager.getPosition(it) }}")
        return closestView
    }


    //创建滚动执行器
    override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller {
        consoleLog("createScroller")
        return object : LinearSmoothScroller(context) {

            //计算将 View 滚动到中心所需的水平偏移量
            override fun calculateDxToMakeVisible(view: View, snapPreference: Int): Int {
                if (!isHorizontal) return 0

                val parentWidth = layoutManager.width - layoutManager.paddingLeft - layoutManager.paddingRight
                val parentCenterX = parentWidth / 2
                val childCenterX = (view.left + view.right) / 2
                return childCenterX - parentCenterX
            }


            //计算将 View 滚动到中心所需的垂直偏移量
            override fun calculateDyToMakeVisible(view: View, snapPreference: Int): Int {
                if (isHorizontal) return 0

                val parentHeight = layoutManager.height - layoutManager.paddingTop - layoutManager.paddingBottom
                val parentCenterY = parentHeight / 2
                val childCenterY = (view.top + view.bottom) / 2
                return childCenterY - parentCenterY
            }


            //实现线性减速效果
            override fun calculateTimeForScrolling(dx: Int): Int {
                val velocity = abs(lastFlingVelocity)
                val distance = abs(dx)

                //速度越快减速时间越长
                val avgVelocity = (velocity + 200f) / 2f
                val calculatedTime = if (avgVelocity > 0) {
                    (distance / avgVelocity * 1000f).toInt()
                } else {
                    (distance / BASE_SPEED * 1000f).toInt()
                }

                return calculatedTime
                    .coerceAtLeast(MIN_SCROLL_TIME)
                    .coerceAtMost(MAX_SCROLL_TIME)
            }


            //重写此方法以应用减速插值器
            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
                val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toInt()
                val time = calculateTimeForScrolling(distance)
                if (time > 0) {
                    action.update(dx, dy, time, DecelerateInterpolator(1.5f))
                }
            }
        }
    }


    //计算最终对齐所需的偏移距离
    override fun calculateDistanceToFinalSnap(layoutManager: RecyclerView.LayoutManager,targetView: View):IntArray {
        consoleLog("calculateDistanceToFinalSnap")

        val out = IntArray(2)

        updateCenterCache(layoutManager)

        if (isHorizontal) {
            val childCenterX = (targetView.left + targetView.right) / 2
            out[0] = childCenterX - cachedCenterX
        } else {
            val childCenterY = (targetView.top + targetView.bottom) / 2
            out[1] = childCenterY - cachedCenterY
        }

        consoleLog("calculateDistanceToFinalSnap: dx=${out[0]}, dy=${out[1]}")
        return out
    }


    //计算目标页码
    private fun calculateTargetPage(currentPos: Int, velocity: Int, itemCount: Int): Int {
        //计算翻页数
        val pageOffset = (velocity / VELOCITY_TO_PAGE_RATIO).toInt()
            .coerceIn(-MAX_PAGES_TO_SNAP, MAX_PAGES_TO_SNAP)

        val targetPos = when {
            velocity > 0 -> currentPos + pageOffset
            velocity < 0 -> currentPos + pageOffset
            else -> currentPos
        }

        consoleLog("calculateTargetPage: currentPos=$currentPos, velocity=$velocity, pageOffset=$pageOffset, targetPos=$targetPos")
        return targetPos.coerceIn(0, itemCount - 1)
    }


    //中心位置缓存
    private fun updateCenterCache(layoutManager: RecyclerView.LayoutManager) {
        cachedCenterX = (layoutManager.width - layoutManager.paddingLeft - layoutManager.paddingRight) / 2
        cachedCenterY = (layoutManager.height - layoutManager.paddingTop - layoutManager.paddingBottom) / 2
    }

    //日志
    private fun consoleLog(msg: String, mark: Boolean = true) {
        if (mark) {
            Log.d("SuMing", "LinearDecelerateSnapHelper: $msg")
        }
    }
}