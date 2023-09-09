# SwipeCardLayout
继承于RecyclerView，实现了卡片堆叠切换效果

* 支持首尾无限循环切换属性
* 支持跟手/不跟手属性
* 支持单向/双向切换（选择不同的LayoutManager）,单向：左滑右滑都是切换下一张卡片；双向：左滑是下一张，右滑是上一张
* 自定义动画(duration, interpolator, alpha、scale、translation, roration....)
* 支持多种卡片类型
* 支持childView回收复用

**属性**

|  属性名   | 类型  | 释义  |
|  ----  | ----  |----  |
| displayCount  | int |最大可见卡片数 |
| swipeThreshold  | float |单位px, 切换卡片滑动阈值 |
| enableInfinite  | boolean |是否开启无限循环 |
| enableCacheMoveState  | boolean |是否开启卡片跟手滑动 |

## 使用

像使用普通RecyclerView一样and
1. 构造数据列表
2. setAdapter
3. setLayoutManager, 单向切换选择SingleDirectionLayoutManager，双向选择MultipleDirectionLayoutManager
4. setCardTransformer, 并重写相关函数,自定义动画数据
以下是一个例子：</br>
```java
inner class TestCardTransformer : CardTransformer() {
    private val defaultShowCount = 3
    private val baseScale = 0.92f
    private val baseTranslateY = 40f
    private val tempConfig = CardLocationConfig()
    override fun locateCard(card: View, position: Int) {
        locate(card, getLocationConfigByPosition(position))
        if (position == defaultShowCount) {
            card.alpha = 0f
        }
    }

    override fun transformCard(card: View, position: Int, percent: Float) {
        val diffConfig = getDiffLocationConfigByPosition(position)
        val locationConfig = getLocationConfigByPosition(position)
        tempConfig.scaleX = locationConfig.scaleX!! + diffConfig.scaleX!! * percent
        tempConfig.scaleY = tempConfig.scaleX
        tempConfig.translationX =
            locationConfig.translationX!! + diffConfig.translationX!! * percent
        tempConfig.translationY =
            locationConfig.translationY!! + diffConfig.translationY!! * percent
        locate(card, tempConfig)

        card.alpha = when {
            (position > defaultShowCount) -> 0f
            (position == defaultShowCount) -> percent
            else -> 1f
        }
    }

    override fun locate(card: View, config: CardLocationConfig) {
        super.locate(card, config)
        card.scaleX = config.scaleX!!
        card.scaleY = config.scaleY!!
        card.translationX = config.translationX!!
        card.translationY = config.translationY!!
    }

    override fun getLocationConfigByPositionInternal(position: Int): CardLocationConfig {
        val config = CardLocationConfig()
        if (position == -1) {
            config.translationX = -getScreenWidth().toFloat()
            config.translationY = 0f
            config.scaleX = 1f
            config.scaleY = 1f
            return config
        }
        config.scaleX = Math.pow(baseScale.toDouble(), position.toDouble()).toFloat()
        config.scaleY = config.scaleX
        config.translationY = - baseTranslateY * position - ContextUtils.dp2px(this@MainActivity, 400f) * ((1 - config.scaleX!!) / 2f)
        config.translationX = 0f
        return config
    }

    private fun getScreenWidth(): Int {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        return dm.widthPixels
    }
}
```
或者可以直接参考项目中MainActivity中的实现</br>
最后实现效果如下：</br>
 <img src="http://tva1.sinaimg.cn/large/006nwaiFgy1hhqsi1hjj5g30ge0zku0z.gif" width = "270" height = "480" alt="图片名称" align=center /> </br>

