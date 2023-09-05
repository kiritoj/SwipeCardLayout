package com.taoke.corelibrary

/**
 * @author taokeyuan
 *
 * 每个卡片的初始定位参数, 更多属性可拓展
 */
class CardLocationConfig {
    var translationX: Float? = null
    var translationY: Float? = null
    var scaleX: Float? = null
    var scaleY: Float? = null
    var alpha: Float? = null
    var rotation: Float? = null

    operator fun minus(config: CardLocationConfig): CardLocationConfig {
        val result = CardLocationConfig()
        if (translationX != null && config.translationX != null) {
            result.translationX = translationX!! - config.translationX!!
        }
        if (translationY != null && config.translationY != null) {
            result.translationY = translationY!! - config.translationY!!
        }
        if (scaleX != null && config.scaleX != null) {
            result.scaleX = scaleX!! - config.scaleX!!
        }
        if (scaleY != null && config.scaleY != null) {
            result.scaleY = scaleY!! - config.scaleY!!
        }
        if (alpha != null && config.alpha != null) {
            result.alpha = alpha!! - config.alpha!!
        }
        if (rotation != null && config.rotation != null) {
            result.rotation = rotation!! - config.rotation!!
        }
        return result
    }
}