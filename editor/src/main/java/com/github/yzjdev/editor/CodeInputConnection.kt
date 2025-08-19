package com.github.yzjdev.editor

import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.min

class CodeInputConnection(val editor: CodeEditText) : BaseInputConnection(editor, true) {
    val et = TextView(editor.context)
    val TAG = "aaa"
    var debug = true
    fun log(vararg msg: Any) {
        if (!debug) return
        val sb = StringBuilder()
        val methodName = Thread.currentThread().stackTrace[3].methodName
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        val time = now.format(formatter)
        sb.append(time)
            .append(" ")
            .append(methodName)
            .append("\n")
        msg.forEach {
            sb.append(it)
                .append("\n")
        }
        Log.d(TAG, "$sb")
    }


    private val context = editor.context
    private val imm = context.getSystemService(InputMethodManager::class.java)


    override fun clearMetaKeyStates(states: Int): Boolean {
        log(states)
        return editor.clearMetaKeyStates(states)
    }


    override fun commitText(
        text: CharSequence?,
        newCursorPosition: Int
    ): Boolean {
        log()
        editor.insert(text?.toString() ?: "")
        return true
    }


    override fun getExtractedText(
        request: ExtractedTextRequest?,
        flags: Int
    ): ExtractedText {
        editor.extractedTextRequest = request
        val et = ExtractedText()
        editor.apply {
            et.text = text
            et.selectionStart = selectionStart
            et.selectionEnd = selectionEnd
            if (isShiftOn) et.flags = et.flags or ExtractedText.FLAG_SELECTING
        }
        return et
    }

    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {

        editor.apply {
            if (cursor >= length) return false
            doc.replace(cursor, 1, "")
            invalidate()
            return true
        }

    }

    override fun getSelectedText(flags: Int): CharSequence? {
        val a = editor.minSelection
        val b = editor.maxSelection
        if (a == b || a < 0) return null
        return TextUtils.substring(editor.text, a, b)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        val a = editor.minSelection
        var b = editor.maxSelection
        if (b < 0) {
            b = 0
        }
        val end = min(b + n, editor.length)

        return TextUtils.substring(editor.text, b, end)
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        val a = editor.minSelection
        val b = editor.maxSelection
        if (a <= 0) return ""
        var length = n
        if (length > a) {
            length = a
        }
        return TextUtils.substring(editor.text, a - length, a)
    }

    override fun performContextMenuAction(id: Int): Boolean {

        return when (id) {
            android.R.id.selectAll -> {
                editor.selectAll()
                log("全选")
                true
            }

            android.R.id.copy -> {
                editor.copy()
                log("复制")
                true
            }

            android.R.id.cut -> {
                editor.cut()
                log("剪切")
                true
            }

            android.R.id.paste,
            android.R.id.pasteAsPlainText -> {
                editor.paste()
                log("粘贴")
                true
            }

            else -> false
        }
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        super.sendKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            return editor.handleKeyDown(event)
        }
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        log("$start $end")
        if (editor.length == 0) return false
        if (isSougouInput()) return true
        val len = editor.length
        if (start > len || end > len || start < 0 || end < 0) {
            return true
        }
        editor.setSelection(start, end)
        return true
    }


    fun isSougouInput(): Boolean {
        val packageName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            imm.currentInputMethodInfo?.packageName
        } else {
            // 兼容旧版本
            val id = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            )
            id?.split("/")?.getOrNull(0)
        }
        return packageName?.contains("com.sohu.inputmethod.sogou") ?: false// 搜狗输入法包名
    }


}