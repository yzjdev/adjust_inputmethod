package com.github.yzjdev.editor.inputmethod

import com.github.yzjdev.editor.CodeEditText

class XunfeiInputConnection(editor: CodeEditText) : EditableInputConnection(editor) {
    //total  chars 1_000_000  max chars  519_843
    override val maxChars: Int
        get() = 500_000


    override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
        editor.apply {
            deleteAfter()
        }
        return true
    }
}