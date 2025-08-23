package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.github.yzjdev.utils.Utils

class App : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var app: Context
    }

    override fun onCreate() {
        super.onCreate()
        app = applicationContext
        Utils.init(this)
    }
}