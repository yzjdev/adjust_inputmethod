package com.github.yzjdev.editor.inputmethod

import com.github.yzjdev.editor.CodeEditText

class BaiduInputConnection(editor: CodeEditText) : EditableInputConnection(editor) {

    override val maxChars: Int
        get() = 259_795

}