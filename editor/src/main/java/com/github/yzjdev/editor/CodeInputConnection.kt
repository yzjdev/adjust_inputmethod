package com.github.yzjdev.editor

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
        request: ExtractedTextRequest,
        flags: Int
    ): ExtractedText {
        log()
        editor.extractedTextRequest = request
        val et = ExtractedText()
        editor.extractedText(et)
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
        if (a == b || a < 0) return ""
        return TextUtils.substring(editor.text, a, b)
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        log(n)
        val a = editor.minSelection
        var b = editor.maxSelection
        if (b < 0) {
            b = 0
        }
        val end = min(b + n, editor.length)
        return TextUtils.substring(editor.text, b, end)
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        log(n)
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
        log(id)
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
            log(event)
            return editor.handleKeyDown(event)
        }
        return false
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        log("$start  $end  $currentInputMethod")
        editor.apply {
            when {
                isXunfeiInput -> setSelection(cursor, cursor)
            }
        }
        return true
    }


    val currentInputMethod: String
        get() = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )

    val isSougouInput: Boolean get() = currentInputMethod.startsWith("com.sohu.inputmethod.sogou")


    val isBaiduInput: Boolean get() = currentInputMethod.startsWith("com.baidu.input")


    val isXunfeiInput: Boolean get() = currentInputMethod.startsWith("com.iflytek.inputmethod")

}