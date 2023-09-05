package com.taoke.corelibrary

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView

/**
 * @author taokeyuan
 * [SwipeCardLayout] 配套LayoutManager基类
 */
abstract class AbsSwipeCardLayoutManager : RecyclerView.LayoutManager() {

    /**
     * 从Down到Up单次滑动距离
     */
    protected var scrollDistance = 0

    /**
     * 存储滑动过程中, 哪些child需要做动画
     */
    protected val animChildList = ArrayList<View>()

    /**
     * 与[animChildList]一一对应, 每一个value的含义是:Child ViewHolder bind的下标与当前的selectedIndex的差值
     */
    protected val animDisplayIndexList = ArrayList<Int>()

    protected var animator = ValueAnimator()
    protected val recycler by lazy { getAttachRecycler() }
    protected lateinit var cardLayout: SwipeCardLayout

    protected var duration = 300L
    private var interpolator: Interpolator = LinearInterpolator()

    /**
     * 当前选中的child是[SwipeCardLayout]的第几个child
     */
    protected var childAnchorIndex = 0

    companion object {
        const val INVALID_CHILD_ANCHOR_INDEX = -1
    }

    fun attachToRecyclerView(recyclerView: SwipeCardLayout) {
        this.cardLayout = recyclerView
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT)
    }

    override fun canScrollHorizontally(): Boolean {
        return true
    }

    open fun canScrollHorizontally(direction: Int): Boolean{
        return false
    }

    /**
     * 松手时选择切换卡片策略
     */
    abstract fun getSwipeType(): Int

    /**
     * 动态切换卡片, 计算percent, 调用[transformCardInternal]
     */
    abstract fun transformCard(@SwipeType swipeType: Int)

    /**
     * 真正控制card动画的
     */
    protected abstract fun transformCardInternal(percent: Float)

    fun setAnimDuration(duration: Long) {
        this.duration = duration
    }

    fun setInterpolator(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    protected fun calculateDisplayAnchorIndex(): Int {
        for (index in childCount - 1 downTo 0) {
            val layoutPosition = cardLayout.getChildLayoutPosition(cardLayout.getChildAt(index))
            if (layoutPosition == cardLayout.getSelectedIndex()) {
                return index
            }
        }
        return INVALID_CHILD_ANCHOR_INDEX
    }

    private fun getAttachRecycler(): RecyclerView.Recycler {
        return cardLayout.getRecycler()
    }

    @IntDef(SwipeType.SWIPE_NEXT, SwipeType.SWIPE_PRE, SwipeType.SWIPE_NOT_CHANGE, SwipeType.SWIPE_NONE)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class SwipeType {
        companion object {
            /**
             * 切换到下一页
             */
            const val SWIPE_NEXT = -1

            /**
             * 切换到上一页
             */
            const val SWIPE_PRE = 1

            /**
             * 滑动了一部分, 但没有达到切换的阈值, 回到原来位置
             */
            const val SWIPE_NOT_CHANGE = 0

            /**
             * 不切换, 没有开启滑动跟手, 又没有达到滑动阈值
             */
            const val SWIPE_NONE = 2
        }
    }

    @IntDef(MoveType.MOVE_LEFT, MoveType.MOVE_RIGHT)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class MoveType {
        companion object {
            /**
             * 向左滑动
             */
            const val MOVE_LEFT = 0

            /**
             * 向右滑动
             */
            const val MOVE_RIGHT = 1
        }
    }

    fun isSwipeCardRunning(): Boolean {
        return animator.isRunning
    }

    fun insertCard(position: Int, index: Int = -1): View{
        val card = recycler.getViewForPosition(position)
        addView(card, index)
        measureChildWithMargins(card, 0, 0)
        val widthSpace = width - getDecoratedMeasuredWidth(card)
        val heightSpace = height - getDecoratedMeasuredHeight(card)
        layoutDecoratedWithMargins(
            card,
            widthSpace / 2,
            heightSpace / 2,
            widthSpace / 2 + getDecoratedMeasuredWidth(card),
            heightSpace / 2 + getDecoratedMeasuredHeight(card)
        )
        return card
    }

    fun cancelSwipeAnim() {
        animator.cancel()
    }

}