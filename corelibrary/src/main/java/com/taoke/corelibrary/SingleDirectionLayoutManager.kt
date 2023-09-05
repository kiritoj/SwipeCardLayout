package com.taoke.corelibrary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taoke.corelibrary.log.Logger
import kotlin.math.abs

class SingleDirectionLayoutManager : AbsSwipeCardLayoutManager() {

    private var currentSwipeType: Int? = null
    private val log = Logger("SingleDirectionLayoutManager")
    private var animCancel = false

    private val animatorListener = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator?) {
            super.onAnimationEnd(animation)
            scrollDistance = 0
            if (currentSwipeType == SwipeType.SWIPE_NOT_CHANGE || animCancel) {
                return
            }
            cardLayout.startInterceptReqLayout()
            if (currentSwipeType == SwipeType.SWIPE_NEXT) {
                //回收last select的child
                removeAndRecycleViewAt(childAnchorIndex, recycler)
                //新增新的child
                val lastDisplayIndex = cardLayout.getChildLayoutPosition(cardLayout.getChildAt(0))
                var insertIndex = -1
                if (lastDisplayIndex < itemCount - 1) {
                    insertIndex = lastDisplayIndex + 1
                } else if (lastDisplayIndex == itemCount - 1 && cardLayout.canSwipeInfinite()) {
                    insertIndex = 0
                }
                if (insertIndex < 0) {
                    childAnchorIndex = calculateDisplayAnchorIndex()
                    cardLayout.stopInterceptReqLayout(false)
                    return
                }
                val card = insertCard(insertIndex, 0)
                cardLayout.locateCard(card, cardLayout.getDisplayCount())
                childAnchorIndex = calculateDisplayAnchorIndex()
                cardLayout.stopInterceptReqLayout(false)
            }
        }

        override fun onAnimationCancel(animation: Animator?) {
            super.onAnimationCancel(animation)
            requestLayout()
            animCancel = true
        }

        override fun onAnimationStart(animation: Animator?) {
            super.onAnimationStart(animation)
            animCancel = false
        }
    }

    init {
        animator.addListener(animatorListener)
    }

    override fun getSwipeType(): Int {
        if (abs(scrollDistance) > cardLayout.getSwipeThreshold()
            && (cardLayout.getSelectedIndex() < itemCount - 1 || cardLayout.canSwipeInfinite())
        ) {
            return SwipeType.SWIPE_NEXT
        }
        return if (cardLayout.enableCacheMoveState() && scrollDistance != 0)
            SwipeType.SWIPE_NOT_CHANGE else SwipeType.SWIPE_NONE
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        detachAndScrapAttachedViews(recycler)
        val firstLayoutPos = cardLayout.getSelectedIndex()
        val lastLayoutPos = cardLayout.getSelectedIndex() + cardLayout.getDisplayCount()
        //无法开启无限循环,只能叠0 ～ dataList.size范围内卡片
        if (!cardLayout.enableSwipeInfinite()) {
            for (i in Math.min(itemCount - 1, lastLayoutPos) downTo firstLayoutPos) {
                val card = insertCard(i)
                cardLayout.locateCard(card, i - cardLayout.getSelectedIndex())
                if (i == cardLayout.getSelectedIndex()) {
                    childAnchorIndex = childCount - 1
                }
            }
            return
        }
        for (index in lastLayoutPos downTo firstLayoutPos) {
            var targetPos = index
            var displayIndex = index - cardLayout.getSelectedIndex()
            if (index >= itemCount) {
                targetPos = index - itemCount
            }
            val card = insertCard(targetPos)
            cardLayout.locateCard(card, displayIndex)
            if (index == cardLayout.getSelectedIndex()) {
                childAnchorIndex = childCount - 1
            }
        }
    }

    override fun transformCard(swipeType: Int) {
        log.d("transformCard, swipeType:$swipeType")
        currentSwipeType = swipeType
        var startPercent = 0f
        var endPercent = 0f
        var newSelectedIndex = cardLayout.getSelectedIndex()
        when (swipeType) {
            SwipeType.SWIPE_NEXT -> {
                startPercent = if (cardLayout.enableCacheMoveState()) scrollDistance / width.toFloat() else 0f
                endPercent = if (scrollDistance > 0) 1f else -1f
                newSelectedIndex++
                if (newSelectedIndex >= itemCount && cardLayout.canSwipeInfinite()) {
                    newSelectedIndex = 0
                }
            }
            SwipeType.SWIPE_NOT_CHANGE -> {
                startPercent = scrollDistance / width.toFloat()
                endPercent = 0f
            }
        }
        cardLayout.setSelectedIndex(newSelectedIndex)
        animator.setFloatValues(startPercent, endPercent)
        animator.duration = duration
        animator.addUpdateListener {
            transformCardInternal(it.animatedValue as Float)
        }
        animator.start()
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        log.d("scrollHorizontallyBy, dx:$dx")
        var canTrans = false
        val lastScrollDistance = scrollDistance
        if (cardLayout.getSelectedIndex() < itemCount - 1 || cardLayout.canSwipeInfinite()) {
            canTrans = true
            scrollDistance = if ((scrollDistance + dx) >= 0) Math.min(width, scrollDistance + dx) else Math.max(-width, scrollDistance + dx)
        }
        if (canTrans && cardLayout.enableCacheMoveState()) {
            transformCardInternal(scrollDistance / width.toFloat())
        }
        return scrollDistance - lastScrollDistance
    }

    /**
     * @param percent 范围为[0,1]左滑, 或者[-1,0]右滑
     */
    override fun transformCardInternal(percent: Float) {
        animChildList.clear()
        animDisplayIndexList.clear()
        for (index in childAnchorIndex downTo 0) {
            animChildList.add(cardLayout.getChildAt(index))
            animDisplayIndexList.add(childAnchorIndex - index)
        }
        for (i in 0 until animChildList.size) {
            cardLayout.transformCard(animChildList[i], animDisplayIndexList[i], percent)
        }
    }
}
