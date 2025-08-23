package com.github.yzjdev.editor

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val TAG = "aaa"

var debug = true
fun log(vararg msg: Any) {
    if (!debug) return
    val sb = StringBuilder()
    val methodName = Thread.currentThread().stackTrace[3].methodName
    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    val time = now.format(formatter)
    sb.append("[ $time ]")
        .append(" -> ")
        .append("$methodName")
        .append("\n")
    msg.forEach {
        sb.append("    $it")
            .append("\n")
    }
    Log.d(TAG, "$sb")
}



