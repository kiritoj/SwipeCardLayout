package com.taoke.corelibrary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import androidx.recyclerview.widget.RecyclerView
import com.taoke.corelibrary.log.Logger

class MultipleDirectionLayoutManager : AbsSwipeCardLayoutManager() {

    private var currentSwipeType: Int? = null
    private val log = Logger("MultipleDirectionLayoutManager")
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
                //回收最后一个child,如果有
                if (childAnchorIndex < childCount - 1) {
                    //detach 可能不会刷新ui, 必须remove掉
                    removeAndRecycleViewAt(childAnchorIndex + 1, recycler)
                }
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
            }

            var removeItem = false
            if (currentSwipeType == SwipeType.SWIPE_PRE) {
                if (childAnchorIndex >= cardLayout.getDisplayCount()) {
                    removeAndRecycleViewAt(0, recycler)
                    removeItem = true
                }
                //新增新的child
                val firstDisplayIndex = cardLayout.getChildLayoutPosition(cardLayout.getChildAt(if (removeItem) childAnchorIndex else childAnchorIndex + 1))
                var insertIndex = -1
                if (firstDisplayIndex > 0) {
                    insertIndex = firstDisplayIndex - 1
                } else if (firstDisplayIndex ==  0 && cardLayout.canSwipeInfinite()) {
                    insertIndex = itemCount - 1
                }
                if (insertIndex < 0) {
                    childAnchorIndex = calculateDisplayAnchorIndex()
                    cardLayout.stopInterceptReqLayout(false)
                    return
                }
                val card = insertCard(insertIndex)
                cardLayout.locateCard(card, -1)
            }
            childAnchorIndex = calculateDisplayAnchorIndex()
            cardLayout.stopInterceptReqLayout(false)
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

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        detachAndScrapAttachedViews(recycler)
        val firstLayoutPos = cardLayout.getSelectedIndex() - 1
        val lastLayoutPos = cardLayout.getSelectedIndex() + cardLayout.getDisplayCount()
        //无法开启无限循环,只能叠0 ～ dataList.size范围内卡片
        if (!cardLayout.enableSwipeInfinite()) {
            for (i in Math.min(itemCount - 1, lastLayoutPos) downTo Math.max(firstLayoutPos, 0)) {
                val card = insertCard(i)
                cardLayout.locateCard(card, i - cardLayout.getSelectedIndex())
                if (i == cardLayout.getSelectedIndex()) {
                    childAnchorIndex = childCount - 1
                }
            }
            return
        }
        //开启无限循环, 哪一边不够了就从另一边取
        for (index in lastLayoutPos downTo firstLayoutPos) {
            var targetPos = index
            var displayIndex = index - cardLayout.getSelectedIndex()
            if (index >= itemCount) {
                targetPos = index - itemCount
            }
            if (index < 0) {
                targetPos = itemCount + index
                displayIndex = -1
            }
            val card = insertCard(targetPos)
            cardLayout.locateCard(card, displayIndex)
            if (index == cardLayout.getSelectedIndex()) {
                childAnchorIndex = childCount - 1
            }
        }
    }

    override fun getSwipeType(): Int {
        if (-scrollDistance > cardLayout.getSwipeThreshold()
            && (cardLayout.getSelectedIndex() > 0 || cardLayout.canSwipeInfinite())
        ) {
            return SwipeType.SWIPE_PRE
        }
        if (scrollDistance > cardLayout.getSwipeThreshold()
            && (cardLayout.getSelectedIndex() < itemCount - 1 || cardLayout.canSwipeInfinite())
        ) {
            return SwipeType.SWIPE_NEXT
        }
        return if (cardLayout.enableCacheMoveState() && scrollDistance != 0)
            SwipeType.SWIPE_NOT_CHANGE else SwipeType.SWIPE_NONE
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        log.d("scrollHorizontallyBy, dx:$dx")
        var canTrans = false
        val lastScrollDistance = scrollDistance
        if (dx > 0) {
            //左滑
            if (cardLayout.getSelectedIndex() < itemCount - 1 || cardLayout.canSwipeInfinite()) {
                canTrans = true
                scrollDistance = Math.min(width, scrollDistance + dx)
            } else if (scrollDistance < 0) {
                //已经到最末尾且无法循环切换, 但是先右滑了一部分, 可以回退一部分
                canTrans = true
                scrollDistance = Math.min(0, scrollDistance + dx)
            }
        }
        if (dx < 0) {
            if (cardLayout.getSelectedIndex() > 0 || cardLayout.canSwipeInfinite()) {
                canTrans = true
                scrollDistance = Math.max(-width, scrollDistance + dx)
            } else if (scrollDistance > 0) {
                canTrans = true
                scrollDistance = Math.max(0, scrollDistance + dx)
            }
        }
        if (canTrans && cardLayout.enableCacheMoveState()) {
            val percent = if (scrollDistance >= 0) scrollDistance / width.toFloat() else 1 + scrollDistance / width.toFloat()
            transformCardInternal(percent)
        }
        return scrollDistance - lastScrollDistance
    }

    override fun transformCard(swipeType: Int) {
        log.d("transformCard, swipeType:$swipeType")
        currentSwipeType = swipeType
        var startPercent = 0f
        var endPercent = 0f
        var newSelectedIndex = cardLayout.getSelectedIndex()
        when (swipeType) {
            SwipeType.SWIPE_PRE -> {
                // 右滑切换上一页, 处理一下, 当成下一个card正在左滑切换, 但不让它切换, percent回到0
                startPercent = if (cardLayout.enableCacheMoveState()) 1 + scrollDistance / width.toFloat() else 1f
                endPercent = 0f
                newSelectedIndex--
                // 无限循环从另一头取
                if (newSelectedIndex < 0 && cardLayout.canSwipeInfinite()) {
                    newSelectedIndex = itemCount - 1
                }
            }
            SwipeType.SWIPE_NEXT -> {
                startPercent = if (cardLayout.enableCacheMoveState()) scrollDistance / width.toFloat() else 0f
                endPercent = 1f
                newSelectedIndex++
                if (newSelectedIndex >= itemCount && cardLayout.canSwipeInfinite()) {
                    newSelectedIndex = 0
                }
            }
            SwipeType.SWIPE_NOT_CHANGE -> {
                startPercent = if (scrollDistance > 0) scrollDistance / width.toFloat() else 1 + scrollDistance / width.toFloat()
                endPercent = if (scrollDistance > 0) 0f else 1f
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

    override fun transformCardInternal(percent: Float) {
        animChildList.clear()
        animDisplayIndexList.clear()
        for (index in childAnchorIndex downTo 0) {
            animChildList.add(cardLayout.getChildAt(index))
            // 右滑的时候做一个转换, 当成左滑来处理
            // 将当前选中的card当前下一个card
            val displayIndex = if (scrollDistance >= 0) childAnchorIndex - index else childAnchorIndex - index + 1
            animDisplayIndexList.add(displayIndex)
        }
        if (childAnchorIndex < childCount - 1 && scrollDistance < 0) {
            animChildList.add(cardLayout.getChildAt(childAnchorIndex + 1))
            animDisplayIndexList.add(0)
        }
        for (i in 0 until animChildList.size) {
            cardLayout.transformCard(animChildList[i], animDisplayIndexList[i], percent)
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        if (cardLayout.childCount == 0) {
            return false
        }
        if (direction < 0) {
            return cardLayout.getSelectedIndex() > 0 || scrollDistance > 0
        }
        return cardLayout.getSelectedIndex() < itemCount - 1 || scrollDistance < 0
    }
}