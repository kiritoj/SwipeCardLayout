package com.taoke.corelibrary

import android.util.SparseArray
import android.view.View

/**
 * @author taokeyuan
 *
 */
abstract class CardTransformer {

    protected val cardLocationConfigMap = SparseArray<CardLocationConfig>()
    protected val diffLocationConfigMap = SparseArray<CardLocationConfig>()


    /**
     * 卡片静态定位
     * @param card 卡片View
     * @param position 卡片position, 当前展示项为1, 左减右加
     */
    open fun locateCard(card: View, position: Int) {

    }

    /**
     * [SingleDirectionLayoutManager], percent取值为[0,1]或[-1,0]
     * [MultipleDirectionLayoutManager], percent 取值始终为[0,1]
     */
    open fun transformCard(card: View, position: Int, percent: Float) {

    }

    open fun locate(card: View, config: CardLocationConfig) {

    }

    fun getLocationConfigByPosition(position: Int): CardLocationConfig{
        var config = cardLocationConfigMap.get(position)
        if (config == null) {
            config = getLocationConfigByPositionInternal(position)
            cardLocationConfigMap.put(position, config)
        }
        return config
    }

    protected open fun getLocationConfigByPositionInternal(position: Int): CardLocationConfig{
        return CardLocationConfig()
    }

    fun getDiffLocationConfigByPosition(position: Int): CardLocationConfig {
        var config = diffLocationConfigMap.get(position)
        if (config == null) {
            val preConfig = getLocationConfigByPosition(position - 1)
            val curConfig = getLocationConfigByPosition(position)
            config = preConfig - curConfig
            diffLocationConfigMap.put(position, config)
        }
        return config
    }
}