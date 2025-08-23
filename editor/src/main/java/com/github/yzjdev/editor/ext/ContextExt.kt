package com.github.yzjdev.editor.ext

import android.content.Context
import android.provider.Settings

val Context.currentInputMethod: String
    get() = Settings.Secure.getString(
        contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )



val Context.isSougouInput: Boolean get() = currentInputMethod.startsWith("com.sohu.inputmethod.sogou")


val Context.isBaiduInput: Boolean get() = currentInputMethod.startsWith("com.baidu.input")


val Context.isXunfeiInput: Boolean get() = currentInputMethod.startsWith("com.iflytek.inputmethod")
