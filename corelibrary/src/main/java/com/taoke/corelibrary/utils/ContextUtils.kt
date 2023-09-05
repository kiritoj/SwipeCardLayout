package com.taoke.corelibrary.utils

import android.content.Context
import android.util.TypedValue

class ContextUtils {
    companion object {
        fun dp2px(context: Context, dp: Float): Int {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
        }
    }
}