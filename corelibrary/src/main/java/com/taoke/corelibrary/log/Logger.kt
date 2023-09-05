package com.taoke.corelibrary.log

import android.util.Log
import java.util.Objects

/**
 * @author taokeyuan
 * 系统Log包装类
 */
class Logger constructor(var tag: String) {

    fun v (format: String?, vararg args: Objects?) {
        Log.v(tag, formatLogMessage(format, *args))
    }

    fun d (format: String?, vararg args: Objects?) {
        Log.d(tag, formatLogMessage(format, *args))
    }

    fun i (format: String?, vararg args: Objects?) {
        Log.i(tag, formatLogMessage(format, *args))
    }

    fun w (format: String?, vararg args: Objects?) {
        Log.w(tag, formatLogMessage(format, *args))
    }

    fun e (format: String?, vararg args: Objects?) {
        Log.e(tag, formatLogMessage(format, *args))
    }

    private fun formatLogMessage(format: String?, vararg args: Objects?) : String {
        if (format?.isNotEmpty() != false) {
            return args.joinToString {
                it?.toString() ?: "null"
            }
        }
        if (args.isEmpty()) {
            return format
        }
        return try {
            String.format(format, args)
        } catch (e: Exception) {
            "$format, 格式化失败${e.message}"
        }
    }
}