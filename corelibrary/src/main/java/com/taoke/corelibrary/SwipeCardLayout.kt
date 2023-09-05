package com.taoke.corelibrary

import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.taoke.corelibrary.log.Logger
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.reflect.Method

/**
 * @author taokeyuan
 *
 * 通用的卡片组件, 继承于RecyclerView, 使用习惯完全相同
 * 1、根据业务场景选择[SingleDirectionLayoutManager]或[MultipleDirectionLayoutManager],或自定义LayoutManager
 *   [SingleDirectionLayoutManager] 可参考旧书摘页卡
 *   [MultipleDirectionLayoutManager] 可参考角色详情页
 * 2、[setCardTransformer]实现卡片切换动画
 */
class SwipeCardLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attributeSet, defStyle) {

    private val log = Logger("DragonCardSwipeLayout")

    private var selectedIndex = 0
    private var transformer: CardTransformer? = null
    private var cardLayoutManager: AbsSwipeCardLayoutManager? = null
    private var pageChangeCallback: OnPageChangeCallback? = null

    /**
     * 最大展示可见的卡片张数
     */
    private var displayCount = Int.MAX_VALUE

    /**
     * 滑动阈值单位px, 可根据具体的ui效果倒推
     */
    private var swipeThreshold = Float.MAX_VALUE

    /**
     * 是否开启无限循环
     */
    private var enableInfinite = false

    /**
     * 是否跟手滑动
     */
    private var enableCacheMoveState = true

    //反射调用拦截requestLayout相关函数
    private var startInterceptRequestMethod: Method? = null
    private var stopInterceptRequestMethod: Method? = null

    interface OnPageChangeCallback {
        fun onPageSelected(position: Int)
    }

    init {
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.DragonCardSwipeLayout)
        displayCount = ta.getInt(R.styleable.DragonCardSwipeLayout_displayCount, Int.MAX_VALUE)
        enableInfinite = ta.getBoolean(R.styleable.DragonCardSwipeLayout_enableInfinite, false)
        enableCacheMoveState = ta.getBoolean(R.styleable.DragonCardSwipeLayout_enableCacheMoveState, true)
        ta.recycle()
        //和viewpager2有同样的问题, 滑动到末尾再滑动, 除了当前select的child, 其他的child会突然消失, 通过设置overScrollMode解决
        overScrollMode = OVER_SCROLL_NEVER
        //destroy的时候cancel动画, 防止内存泄漏
        val application = context.applicationContext as? Application
        application?.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                if (activity == context) {
                    cardLayoutManager?.cancelSwipeAnim()
                    application.unregisterActivityLifecycleCallbacks(this)
                }
            }
        })

    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (cardLayoutManager?.isSwipeCardRunning() != false) {
            return false
        }
        if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
            val swipeType = cardLayoutManager!!.getSwipeType()
            if (swipeType != AbsSwipeCardLayoutManager.SwipeType.SWIPE_NONE) {
                cardLayoutManager!!.transformCard(swipeType)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun setLayoutManager(layout: LayoutManager?) {
        if (layout !is AbsSwipeCardLayoutManager) {
            throw IllegalArgumentException("必须使用AbsSwipeCardLayoutManager")
        }
        cardLayoutManager = layout
        layout.attachToRecyclerView(this)
        super.setLayoutManager(layout)
    }

    fun locateCard(card: View, position: Int) {
        transformer?.locateCard(card, position)
    }

    fun transformCard(card: View, position: Int, percent: Float) {
        transformer?.transformCard(card, position, percent)
    }

    fun getRecycler(): Recycler {
        try {
            val recyclerFiled = RecyclerView::class.java.getDeclaredField("mRecycler")
            recyclerFiled.isAccessible = true
            return recyclerFiled.get(this) as Recycler
        } catch (e: Exception) {
            log.e("反射获取Recycler失败, error:${e.message}")
        }
        return Recycler()
    }

    /**
     * getter and setter...
     */
    fun setDisplayCount(count: Int) {
        displayCount = count
    }

    fun getDisplayCount(): Int {
        return displayCount
    }

    fun setSwipeThreshold(threshold: Float) {
        swipeThreshold = threshold
    }

    fun getSwipeThreshold(): Float {
        return swipeThreshold
    }

    fun setSelectedIndex(index: Int, requestLayout: Boolean = false) {
        if (index !in 0 until layoutManager!!.itemCount) {
            throw IllegalArgumentException("selected index 不合法!!")
        }
        if (selectedIndex == index) {
            return
        }
        selectedIndex = index
        if (requestLayout) {
            requestLayout()
        }
        pageChangeCallback?.onPageSelected(selectedIndex)
    }

    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    fun setCardTransformer(transformer: CardTransformer) {
        this.transformer = transformer
    }

    fun setCacheMoveState(enable: Boolean) {
        enableCacheMoveState = enable
    }

    fun enableCacheMoveState(): Boolean {
        return enableCacheMoveState
    }

    fun setEnableSwipeInfinite(enable: Boolean) {
        enableInfinite = enable
    }

    fun enableSwipeInfinite(): Boolean{
        return enableInfinite
    }

    fun canSwipeInfinite(): Boolean {
        if (adapter == null || layoutManager == null) {
            return false
        }
        return layoutManager!!.itemCount > + 2 && enableInfinite
    }

    fun setOnPageChangeCallback(pageChangeCallback: OnPageChangeCallback) {
        this.pageChangeCallback = pageChangeCallback
    }

    fun startInterceptReqLayout() {
        if (startInterceptRequestMethod == null) {
            try {
                startInterceptRequestMethod = javaClass.superclass?.getDeclaredMethod("startInterceptRequestLayout")
                startInterceptRequestMethod?.isAccessible = true
            } catch (e: Exception) {
                log.d("反射获取startInterceptRequestLayout方法失败, error: ${e.message}")
            }
        }
        startInterceptRequestMethod?.invoke(this)
    }

    fun stopInterceptReqLayout(performLayoutChildren: Boolean) {
        if (stopInterceptRequestMethod == null) {
            try {
                stopInterceptRequestMethod = javaClass.superclass?.getDeclaredMethod("stopInterceptRequestLayout", Boolean::class.java)
                stopInterceptRequestMethod?.isAccessible = true
            } catch (e: Exception) {
                log.d("stopInterceptRequestLayout, error: ${e.message}")
            }
        }
        stopInterceptRequestMethod?.invoke(this, performLayoutChildren)
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        //去除fling效果, 避免松手后LayoutManager#scrollHorizontallyBy仍然被调用
        return false
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        return cardLayoutManager?.canScrollHorizontally(direction) ?: false
    }
}