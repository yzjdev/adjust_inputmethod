package com.github.yzjdev.editor.inputmethod

import android.R
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.CorrectionInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputContentInfo
import com.github.yzjdev.editor.CodeEditText
import com.github.yzjdev.editor.ext.currentInputMethod
import com.github.yzjdev.editor.log

open class EditableInputConnection(
    val editor: CodeEditText,
    val context: Context = editor.context
) :
    BaseInputConnection(editor, true) {

    val TAG = "aaa"
    open val maxChars: Int
        get() = editor.length

    val length: Int
        get() = editor.length

    override fun beginBatchEdit(): Boolean {
//        log(context.currentInputMethod)
        return super.beginBatchEdit()
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        log(context.currentInputMethod)
        return super.clearMetaKeyStates(states)
    }

    override fun closeConnection() {
        log(context.currentInputMethod)
        return super.closeConnection()
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        log(context.currentInputMethod)
        return super.commitCompletion(text)
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        log(context.currentInputMethod)
        return super.commitContent(inputContentInfo, flags, opts)
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        log(context.currentInputMethod)
        return super.commitCorrection(correctionInfo)
    }

    override fun commitText(
        text: CharSequence,
        newCursorPosition: Int
    ): Boolean {
        log(context.currentInputMethod, text.length)
        editor.insert(text)
        return true
    }

    override fun deleteSurroundingText(
        beforeLength: Int,
        afterLength: Int
    ): Boolean {
        log(context.currentInputMethod, "$beforeLength  $afterLength")
        return super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun deleteSurroundingTextInCodePoints(
        beforeLength: Int,
        afterLength: Int
    ): Boolean {
        log(context.currentInputMethod)
        return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
    }

    override fun endBatchEdit(): Boolean {
//        log(context.currentInputMethod)
        return super.endBatchEdit()
    }

    override fun finishComposingText(): Boolean {
        log(context.currentInputMethod)
        return super.finishComposingText()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        log(context.currentInputMethod)
        return super.getCursorCapsMode(reqModes)
    }

    override fun getExtractedText(
        request: ExtractedTextRequest,
        flags: Int
    ): ExtractedText? {
        log(context.currentInputMethod)
        editor.extractedRequestToken = request.token
        return editor.getExtractedText()
    }

    override fun getHandler(): Handler? {
//        log(context.currentInputMethod)
        return Handler(Looper.getMainLooper())
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        log(context.currentInputMethod)
        editor.apply {
            if (isShiftOn && selectionStart != selectionEnd) {
                return doc.get(minSelection, maxSelection - minSelection)
            }
        }
        return null
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        log(context.currentInputMethod)
        editor.apply {
            if (cursor < length) return doc.get(cursor, 1)
        }
        return null
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        log(context.currentInputMethod)
        editor.apply {
            if (cursor > 0) return doc.get(cursor - 1, 1)
        }
        return null
    }

    override fun performContextMenuAction(id: Int): Boolean {
        log(context.currentInputMethod)
        editor.apply {
            val z: Boolean = when (id) {
                R.id.selectAll -> {
                    selectAll()
                    true
                }

                R.id.copy -> {
                    copy()
                    true
                }

                R.id.cut -> {
                    cut()
                    true
                }

                R.id.paste,
                R.id.pasteAsPlainText -> {
                    paste()
                    true
                }

                else -> false
            }
            return z
        }

    }

    override fun performEditorAction(editorAction: Int): Boolean {
        log(context.currentInputMethod)
        return super.performEditorAction(editorAction)
    }

    override fun performPrivateCommand(
        action: String?,
        data: Bundle?
    ): Boolean {
        log(context.currentInputMethod)
        return super.performPrivateCommand(action, data)
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        log(context.currentInputMethod)
        return super.reportFullscreenMode(enabled)
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        log(context.currentInputMethod)
        return super.requestCursorUpdates(cursorUpdateMode)
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        log(context.currentInputMethod, event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val z = editor.handleKeyDown(event)
            return z
        }
        return super.sendKeyEvent(event)
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        log(context.currentInputMethod)
        return super.setComposingRegion(start, end)
    }

    override fun setComposingText(
        text: CharSequence?,
        newCursorPosition: Int
    ): Boolean {
        log(context.currentInputMethod)
        return super.setComposingText(text, newCursorPosition)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        log(context.currentInputMethod, "$start  $end  ${editor.length}")
        if (start != end && end - start == maxChars) {
            return performContextMenuAction(android.R.id.selectAll)
        }

        editor.apply {
            isShiftOn = start != end
            cursor = end
            selectionStart = start
            selectionEnd = end
            updateImm()
            scrollToVisible()
            invalidate()
        }
        return true
    }

}