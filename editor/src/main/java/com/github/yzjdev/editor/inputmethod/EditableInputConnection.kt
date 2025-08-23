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
//        log()
        return super.beginBatchEdit()
    }

    override fun clearMetaKeyStates(states: Int): Boolean {
        log(states)
        return super.clearMetaKeyStates(states)
    }

    override fun closeConnection() {
        log()
        return super.closeConnection()
    }

    override fun commitCompletion(text: CompletionInfo?): Boolean {
        log()
        return super.commitCompletion(text)
    }

    override fun commitContent(
        inputContentInfo: InputContentInfo,
        flags: Int,
        opts: Bundle?
    ): Boolean {
        log()
        return super.commitContent(inputContentInfo, flags, opts)
    }

    override fun commitCorrection(correctionInfo: CorrectionInfo?): Boolean {
        log()
        return super.commitCorrection(correctionInfo)
    }

    override fun commitText(
        text: CharSequence,
        newCursorPosition: Int
    ): Boolean {
        log(text.length)
        editor.insert(text)
        return true
    }

    override fun deleteSurroundingText(
        beforeLength: Int,
        afterLength: Int
    ): Boolean {
        log("$beforeLength  $afterLength")
        return super.deleteSurroundingText(beforeLength, afterLength)
    }

    override fun deleteSurroundingTextInCodePoints(
        beforeLength: Int,
        afterLength: Int
    ): Boolean {
        log()
        return super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
    }

    override fun endBatchEdit(): Boolean {
//        log()
        return super.endBatchEdit()
    }

    override fun finishComposingText(): Boolean {
        log()
        return super.finishComposingText()
    }

    override fun getCursorCapsMode(reqModes: Int): Int {
        log()
        return super.getCursorCapsMode(reqModes)
    }

    override fun getExtractedText(
        request: ExtractedTextRequest,
        flags: Int
    ): ExtractedText? {
        log()
        editor.extractedRequestToken = request.token
        return editor.getExtractedText()
    }

    override fun getHandler(): Handler? {
//        log()
        return Handler(Looper.getMainLooper())
    }

    override fun getSelectedText(flags: Int): CharSequence? {
        log()
        editor.apply {
            if (isShiftOn && selectionStart != selectionEnd) {
                return doc.get(minSelection, maxSelection - minSelection)
            }
        }
        return null
    }

    override fun getTextAfterCursor(n: Int, flags: Int): CharSequence? {
        log()
        editor.apply {
            if (cursor < length) return doc.get(cursor, 1)
        }
        return null
    }

    override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence? {
        log()
        editor.apply {
            if (cursor > 0) return doc.get(cursor - 1, 1)
        }
        return null
    }

    override fun performContextMenuAction(id: Int): Boolean {
        log()
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
        log()
        return super.performEditorAction(editorAction)
    }

    override fun performPrivateCommand(
        action: String?,
        data: Bundle?
    ): Boolean {
        log()
        return super.performPrivateCommand(action, data)
    }

    override fun reportFullscreenMode(enabled: Boolean): Boolean {
        log()
        return super.reportFullscreenMode(enabled)
    }

    override fun requestCursorUpdates(cursorUpdateMode: Int): Boolean {
        log()
        return super.requestCursorUpdates(cursorUpdateMode)
    }

    override fun sendKeyEvent(event: KeyEvent): Boolean {
        log(event)
        if (event.action == KeyEvent.ACTION_DOWN) {
            val z = editor.handleKeyDown(event)
            return z
        }
        return super.sendKeyEvent(event)
    }

    override fun setComposingRegion(start: Int, end: Int): Boolean {
        log()
        return super.setComposingRegion(start, end)
    }

    override fun setComposingText(
        text: CharSequence?,
        newCursorPosition: Int
    ): Boolean {
        log()
        return super.setComposingText(text, newCursorPosition)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        log("$start  $end  ${editor.length}")
        if (start != end && end - start == maxChars) {
            return performContextMenuAction(android.R.id.selectAll)
        }

        editor.apply {
            isShiftOn = start != end
            cursor = end
            selectionStart = start
            selectionEnd = end
            refreshEditor()
        }
        return true
    }

}