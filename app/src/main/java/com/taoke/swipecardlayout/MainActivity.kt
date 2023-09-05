package com.taoke.swipecardlayout

import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.taoke.corelibrary.CardLocationConfig
import com.taoke.corelibrary.CardTransformer
import com.taoke.corelibrary.MultipleDirectionLayoutManager
import com.taoke.corelibrary.SwipeCardLayout
import com.taoke.corelibrary.utils.ContextUtils


class MainActivity : AppCompatActivity() {

    private lateinit var swipeCardLayout: SwipeCardLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initSwipeCardLayout()
    }

    private fun initSwipeCardLayout() {
        swipeCardLayout = findViewById<SwipeCardLayout>(R.id.swipe_card_layout).apply {
            setDisplayCount(3)
            setEnableSwipeInfinite(true)
            setCacheMoveState(true)
            setSwipeThreshold(200f)
            setCardTransformer(TestCardTransformer())
        }
        swipeCardLayout.layoutManager = MultipleDirectionLayoutManager().apply {
            setAnimDuration(500)
        }

        swipeCardLayout.adapter = object : RecyclerView.Adapter<CardViewHolder>() {
            val dataList = listOf(0, 1, 2, 3, 4, 5)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
                return CardViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
                )
            }

            override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
                holder.onBind(dataList[position])
            }

            override fun getItemCount(): Int {
                return dataList.size
            }

        }
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text: TextView = itemView.findViewById(R.id.text)
        private val bgColorArray = listOf("#D42C2C", "#2C4DD4", "#2CD47A")

        fun onBind(data: Int) {
            text.text = "这是第${data + 1}张卡片"
            itemView.setBackgroundColor(Color.parseColor(bgColorArray[data % bgColorArray.size]))
        }
    }

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
}