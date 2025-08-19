package com.github.yzjdev.utils

import android.content.Context

object Utils {
    private var appContext : Context ? = null
    val context : Context
        get() = appContext!!


    fun init(context: Context){
        appContext = context.applicationContext

    }
}