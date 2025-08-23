package com.github.yzjdev.editor.inputmethod

import com.github.yzjdev.editor.CodeEditText

class SougouInputConnection(editor: CodeEditText) : EditableInputConnection(editor) {

    override val maxChars: Int
        get() = 300000
    override fun clearMetaKeyStates(states: Int): Boolean {
        if (states == 193){
            editor.apply {
                isShiftOn = false
                cursor = selectionEnd
                selectionStart = cursor
                selectionEnd = cursor
                refreshEditor()
            }
            return true
        }
        return super.clearMetaKeyStates(states)
    }

    override fun setSelection(start: Int, end: Int): Boolean {
        return true
    }

}