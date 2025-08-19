package com.github.yzjdev.utils

import android.annotation.SuppressLint
import android.util.TypedValue

@SuppressLint("StaticFieldLeak")
val app = Utils.context


val Int.dp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            app.resources.displayMetrics
        ).toInt()
    }

val Int.sp: Int
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this.toFloat(),
            app.resources.displayMetrics
        ).toInt()
    }

val Float.dp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            app.resources.displayMetrics
        )
    }

val Float.sp: Float
    get() {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this,
            app.resources.displayMetrics
        )
    }
