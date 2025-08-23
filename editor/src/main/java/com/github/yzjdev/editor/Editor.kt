package com.github.yzjdev.editor
enum class CursorAction{
    ACTION_LEFT,
    ACTION_RIGHT,
    ACTION_UP,
    ACTION_DOWN,
    ACTION_HOME,
    ACTION_END
}
interface Editor {

    //光标移动
    fun moveCursor(action: CursorAction)
    fun moveLeft()
    fun moveRight()
    fun moveUp()
    fun moveDown()
    fun moveHome()
    fun moveEnd()

    //
    fun selectAll()
    fun copy()
    fun cut()
    fun paste()

    fun insert(text: CharSequence)
    fun delete()
    fun deleteAfter()
    fun undo()
    fun redo()

}