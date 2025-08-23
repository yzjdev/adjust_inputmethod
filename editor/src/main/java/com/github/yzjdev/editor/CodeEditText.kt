package com.github.yzjdev.editor

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.InputType
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Scroller
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import com.github.yzjdev.editor.ext.getLineEnd
import com.github.yzjdev.editor.ext.getLineStart
import com.github.yzjdev.editor.ext.getLineText
import com.github.yzjdev.editor.ext.getRealLineLength
import com.github.yzjdev.editor.ext.isBaiduInput
import com.github.yzjdev.editor.ext.isSougouInput
import com.github.yzjdev.editor.ext.isXunfeiInput
import com.github.yzjdev.editor.inputmethod.BaiduInputConnection
import com.github.yzjdev.editor.inputmethod.EditableInputConnection
import com.github.yzjdev.editor.inputmethod.SougouInputConnection
import com.github.yzjdev.editor.inputmethod.XunfeiInputConnection
import com.github.yzjdev.utils.Utils
import com.github.yzjdev.utils.dp
import com.github.yzjdev.utils.sp
import org.eclipse.jface.text.Document
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class CodeEditText : View, Editor {
    val TAG = "aaa"

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    init {
        viewTreeObserver.addOnGlobalLayoutListener {
            if (imm.isActive(this)) scrollToVisible()
        }
    }

    var doc = Document()
    val imm = context.getSystemService(InputMethodManager::class.java)
    var inputConnection = EditableInputConnection(this)

    val scroller = Scroller(context)
    val gestureDetector = GestureDetector(Utils.context, GestureListener(this))


    //光标与选区
    var isShiftOn: Boolean = false
    var cursor: Int = 0
    var selectionStart: Int = cursor
    var selectionEnd: Int = cursor
    val minSelection: Int get() = min(selectionStart, selectionEnd)
    val maxSelection: Int get() = max(selectionStart, selectionEnd)
    var extractedRequestToken: Int = 0


    var baseTextSize = 20f.sp
    var textMaxWidth = width

    //画笔
    val lineNumberBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        alpha = 100
    }
    val textBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        alpha = 60
    }
    val lineNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textAlign = Paint.Align.RIGHT
    }

    val currentLineBackgroundPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        alpha = 60
    }


    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
    }

    val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 2f.dp
    }

    val selectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        alpha = 30

    }

    val paintList = mutableListOf(
        lineNumberBackgroundPaint,
        textBackgroundPaint,
        textPaint,
        lineNumberPaint,
        currentLineBackgroundPaint,
        cursorPaint,
        selectionPaint
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        paintList.forEach {
            it.textSize = baseTextSize
            it.typeface = Typeface.createFromAsset(context.assets, "JetBrainsMono-Regular.ttf")
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制行号背景
        drawLineNumberBackground(canvas)
        //绘制文本背景
        drawTextBackground(canvas)
        //可见行
        val visibleStartLine = max(0, scrollY / lineHeight)
        val visibleEndLine = min(lineCount, (scrollY + height) / lineHeight + 1)
        //绘制行号和文本
        drawText(canvas, visibleStartLine, visibleEndLine)
        //绘制光标
        drawCursor(canvas)
        //绘制选区
        drawSelection(canvas, visibleStartLine, visibleEndLine)
    }

    fun drawLineNumberBackground(canvas: Canvas) {
        val r = Rect()
        r.set(0, scrollY, lineNumberPanelWidth, scrollY + height)
        canvas.drawRect(r, lineNumberBackgroundPaint)
    }

    fun drawTextBackground(canvas: Canvas) {
        val r = Rect()
        r.set(lineNumberPanelWidth, scrollY, width + scrollX, scrollY + height)
        canvas.drawRect(r, textBackgroundPaint)
    }

    fun drawText(canvas: Canvas, visibleStartLine: Int, visibleEndLine: Int) {
        val lineNumberX = lineNumberWidth + lineNumberPadding / 2
        for (i in visibleStartLine until visibleEndLine) {
            val lineTextBaseY = i * lineHeight - textPaint.fontMetricsInt.ascent //文本基线
            // 绘制行号
            canvas.drawText(
                "${i + 1}",
                lineNumberX.toFloat(),
                lineTextBaseY.toFloat(),
                lineNumberPaint
            )
            // 按行绘制文本
            val lineText = doc.getLineText(i)
            canvas.drawText(
                lineText,
                lineNumberPanelWidth.toFloat(),
                lineTextBaseY.toFloat(),
                textPaint
            )
            val w = measureText(lineText)
            if (w > textMaxWidth) textMaxWidth = w
        }
    }

    fun drawCursor(canvas: Canvas) {
        if (isShiftOn) return
        val line = doc.getLineOfOffset(cursor)
        val lineStart = doc.getLineStart(line)
        val text = doc.get(lineStart, cursor - lineStart)
        val x = (lineNumberPanelWidth + measureText(text)).toFloat()
        val y = (line * lineHeight).toFloat()
        canvas.drawLine(x, y, x, y + lineHeight, cursorPaint)
    }

    fun drawSelection(canvas: Canvas, visibleStartLine: Int, visibleEndLine: Int) {
        if (!isShiftOn) return
        val a = minSelection
        val b = maxSelection
        val lineA = doc.getLineOfOffset(a)
        val lineB = doc.getLineOfOffset(b)
        if (lineB < visibleStartLine) return
        if (lineA > visibleEndLine) return
        val lineStartA = doc.getLineStart(lineA)
        val lineStartB = doc.getLineStart(lineB)
        val xA = lineNumberPanelWidth + measureText(lineStartA, a - lineStartA)
        val xB = lineNumberPanelWidth + if (b == lineStartB) 2.dp
        else measureText(lineStartB, b - lineStartB)
        val start = max(lineA, visibleStartLine)
        val end = min(lineB, visibleEndLine)
        val r = Rect() // 复用 Rect
        if (lineA == lineB) {
            // 单行选中
            if (a == b)
                r.set(xA - 1.dp, lineA * lineHeight, xA + 1.dp, lineA * lineHeight + lineHeight)
            else
                r.set(xA, lineA * lineHeight, xB, lineA * lineHeight + lineHeight)
            canvas.drawRect(r, selectionPaint)
        } else {
            // 第一行
            r.set(xA, start * lineHeight, scrollX + width, start * lineHeight + lineHeight)
            canvas.drawRect(r, selectionPaint)
            // 中间整行
            if (start + 1 < end) {
                r.set(
                    lineNumberPanelWidth,
                    (start + 1) * lineHeight,
                    scrollX + width,
                    end * lineHeight
                )
                canvas.drawRect(r, selectionPaint)
            }
            // 最后一行
            r.set(lineNumberPanelWidth, end * lineHeight, xB, end * lineHeight + lineHeight)
            canvas.drawRect(r, selectionPaint)
        }
    }

    //统一用textPaint测量字符宽度
    fun measureText(pos: Int, length: Int) = measureText(doc.get(pos, length))
    fun measureText(text: String) = textPaint.measureText(text).roundToInt()

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.initialSelStart = selectionStart
        outAttrs.initialSelEnd = selectionEnd
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
        if (context.isSougouInput) {
            inputConnection = SougouInputConnection(this)
        } else if (context.isXunfeiInput) {
            inputConnection = XunfeiInputConnection(this)
        } else if (context.isBaiduInput) {
            inputConnection = BaiduInputConnection(this)
        }
        return inputConnection
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidate()
        }
    }

    /**
     * 一些常用方法get  set  实现
     */
    var text: String
        set(value) {
            doc.set(value)
            invalidate()
        }
        get() = doc.get()

    var textSize: Float = baseTextSize
        set(value) {
            field = value.coerceIn(10f.sp, 32f.sp)
            paintList.forEach {
                it.textSize = value
            }
            invalidate()
        }

    val length: Int
        get() = doc.length

    private val lineNumberWidth: Int
        get() = measureText("$lineCount")

    var lineNumberPadding: Int = 16.dp
        set(value) {
            field = value.coerceIn(16.dp, 32.dp)
            invalidate()
        }

    val lineNumberPanelWidth: Int
        get() = lineNumberWidth + lineNumberPadding

    val lineHeight: Int
        get() = textPaint.fontMetricsInt.run {
            bottom - top
        }

    val lineCount: Int
        get() = doc.numberOfLines

    // 计算内容的总宽度
    private val contentWidth: Int
        get() = max(width, textMaxWidth)

    // 计算内容的总高度
    private val contentHeight: Int
        get() = max(height, (lineHeight * lineCount).toInt())

    val maxScrollX: Int
        get() = max(0, contentWidth - width / 2)
    val maxScrollY: Int
        get() = max(0, contentHeight - height / 2)


    /**
     * inputConnection 实现
     */

    fun setSelection(start: Int, end: Int) {
        isShiftOn = start != end
        if (isShiftOn) {
            cursor = end
            selectionStart = start
            selectionEnd = end
        } else {
            cursor = start
            selectionStart = start
            selectionEnd = start
        }
        refreshEditor()
    }

    fun handleKeyDown(event: KeyEvent): Boolean {

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> isShiftOn = event.isShiftPressed

            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT -> isShiftOn = true
        }
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                delete()
                true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                moveLeft()
                true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveRight()
                true
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                moveUp()
                true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                moveDown()
                true
            }

            KeyEvent.KEYCODE_MOVE_HOME -> {
                moveHome()
                true
            }

            KeyEvent.KEYCODE_MOVE_END -> {
                moveEnd()
                true
            }

            KeyEvent.KEYCODE_ENTER -> {
                insert("\n")
                true
            }

            KeyEvent.KEYCODE_TAB -> {
                insert("    ")
                true
            }

            else -> false
        }
    }

    /**
     * imm 实现
     */
    fun refreshEditor() {
        updateImm()
        scrollToVisible()
        invalidate()
    }

    private fun updateImm() {
        updateImmSelection()
        updateImmExtractedText()
    }

    private fun updateImmSelection() {
        imm.updateSelection(this, selectionStart, selectionEnd, -1, -1)
    }

    private fun updateImmExtractedText() {
        imm.updateExtractedText(this, extractedRequestToken, getExtractedText())
    }

    fun getExtractedText(): ExtractedText {
        val et = ExtractedText()
        val maxChars = inputConnection.maxChars
        et.text = if (length > maxChars) text.substring(0, maxChars) else text
        et.startOffset = 0
        et.selectionStart = minSelection
        et.selectionEnd = maxSelection
        if (isShiftOn) et.flags = et.flags or ExtractedText.FLAG_SELECTING
        return et
    }


    /**
     * 实现scrollToVisible
     */
    private fun scrollToVisible() {
        val line = doc.getLineOfOffset(cursor)
        val lineStart = doc.getLineStart(line)
        val len = cursor - lineStart
        var cursorX = lineNumberPanelWidth
        if (cursor != lineStart) {
            cursorX += measureText(lineStart, len)
        }
        val visibleLeft = scrollX
        val visibleRight = scrollX + width

        val cursorTop = line * lineHeight //光标顶部
        val cursorBottom = cursorTop + lineHeight * 2// 光标底部

        val rect = Rect()
        getWindowVisibleDisplayFrame(rect)

        val visibleTop = scrollY
        val visibleBottom = scrollY + rect.height() - rect.top


        var targetX = scrollX
        var targetY = scrollY

        // 水平方向滚动
        val H_MARGIN = lineNumberPanelWidth // 光标左右留一点空隙
        if (cursorX < visibleLeft + H_MARGIN) {
            targetX = (cursorX - H_MARGIN)
        } else if (cursorX > visibleRight - H_MARGIN) {
            targetX = (cursorX + H_MARGIN - width)
        }

        if (cursorTop < visibleTop) {
            targetY = cursorTop
        } else if (cursorBottom > visibleBottom) {
            targetY = (cursorBottom - rect.height() + lineHeight)
        }

        if (targetX != scrollX || targetY != scrollY) {
            scroller.startScroll(
                scrollX,
                scrollY,
                targetX - scrollX,
                targetY - scrollY
            )
            invalidate()
        }
    }


    /**
     * 光标移动实现
     */
    override fun moveCursor(action: CursorAction) {
        //防止光标越界
        when (action) {
            CursorAction.ACTION_LEFT, CursorAction.ACTION_UP -> if (cursor <= 0) return
            CursorAction.ACTION_RIGHT, CursorAction.ACTION_DOWN -> if (cursor >= length) return
            else -> {}
        }

        val currentLine = doc.getLineOfOffset(cursor)
        val currentLineStart = doc.getLineStart(currentLine)
        val currentLineEnd = doc.getLineEnd(currentLine)

        val newCursor: Int = when (action) {
            CursorAction.ACTION_LEFT -> {
                desiredColumn = -1
                if (cursor == currentLineStart) doc.getLineEnd(currentLine - 1)
                else cursor - 1
            }

            CursorAction.ACTION_RIGHT -> {
                desiredColumn = -1
                if (cursor == currentLineEnd) doc.getLineStart(currentLine + 1)
                else cursor + 1
            }

            CursorAction.ACTION_UP -> {
                if (currentLine == 0) {
                    desiredColumn = -1
                    0
                } else {
                    val len = cursor - doc.getLineStart(currentLine)
                    if (desiredColumn == -1) desiredColumn = len
                    val preLineStart = doc.getLineStart(currentLine - 1)
                    val preLineLength = doc.getRealLineLength(currentLine - 1)
                    preLineStart + min(desiredColumn, preLineLength)
                }
            }

            CursorAction.ACTION_DOWN -> {
                if (currentLine == lineCount - 1) {
                    desiredColumn = -1
                    length
                } else {
                    val len = cursor - doc.getLineOffset(currentLine)
                    if (desiredColumn == -1) desiredColumn = len
                    val nextLineStart = doc.getLineStart(currentLine + 1)
                    val nextLineLength = doc.getRealLineLength(currentLine + 1)
                    nextLineStart + min(desiredColumn, nextLineLength)
                }
            }

            CursorAction.ACTION_HOME -> {
                desiredColumn = -1
                0
            }

            CursorAction.ACTION_END -> {
                desiredColumn = -1
                length
            }
        }

        cursor = newCursor
        if (isShiftOn) {
            selectionEnd = newCursor
        } else {
            selectionStart = cursor
            selectionEnd = cursor
        }
        refreshEditor()

    }

    override fun moveLeft() {
        moveCursor(CursorAction.ACTION_LEFT)
    }

    override fun moveRight() {
        moveCursor(CursorAction.ACTION_RIGHT)
    }

    var desiredColumn = -1
    override fun moveUp() {
        moveCursor(CursorAction.ACTION_UP)
    }

    override fun moveDown() {
        moveCursor(CursorAction.ACTION_DOWN)
    }

    override fun moveHome() {
        moveCursor(CursorAction.ACTION_HOME)
    }

    override fun moveEnd() {
        moveCursor(CursorAction.ACTION_END)
    }

    override fun selectAll() {
        isShiftOn = true
        cursor = length
        selectionStart = 0
        selectionEnd = length
        refreshEditor()
    }

    override fun copy() {

        if (selectionStart == selectionEnd) return
        try {
            val text = doc.get(minSelection, maxSelection - minSelection)
            val cm = context.getSystemService(ClipboardManager::class.java)
            val clipData = ClipData.newPlainText("text", text)
            cm.setPrimaryClip(clipData)
            Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show()
            isShiftOn = false
            cursor = selectionEnd
            selectionStart = cursor
            selectionEnd = cursor
            updateImm()
            invalidate()
        } catch (_: Exception) {
            Toast.makeText(context, "${length}个字符，你要撑死我吗", Toast.LENGTH_SHORT).show()
        }
    }

    override fun cut() {

        if (selectionStart == selectionEnd) return
        try {
            val text = doc.get(minSelection, maxSelection - minSelection)
            val cm = context.getSystemService(ClipboardManager::class.java)
            val clipData = ClipData.newPlainText("text", text)
            cm.setPrimaryClip(clipData)
            doc.replace(minSelection, maxSelection - minSelection, "")
            isShiftOn = false
            cursor = minSelection
            selectionStart = cursor
            selectionEnd = cursor
            refreshEditor()
        } catch (_: Exception) {
            Toast.makeText(context, "${length}个字符，你要撑死我吗", Toast.LENGTH_SHORT).show()
        }
    }

    override fun paste() {

        val cm = context.getSystemService(ClipboardManager::class.java)
        val clipData = cm.primaryClip
        val text = clipData?.getItemAt(0)?.text?.toString()
        if (text.isNullOrEmpty()) return
        doc.replace(minSelection, maxSelection - minSelection, text)
        cursor = minSelection + text.length
        selectionStart = cursor
        selectionEnd = cursor
        refreshEditor()
    }

    override fun insert(text: CharSequence) {
        if (isShiftOn) {
            doc.replace(minSelection, maxSelection - minSelection, text.toString())
            isShiftOn = false
            cursor = minSelection + text.length
            selectionStart = cursor
            selectionEnd = cursor
            refreshEditor()
        } else {
            if (text.isNotEmpty()) {
                doc.replace(cursor, 0, text.toString())
                cursor = cursor + text.length
                selectionStart = cursor
                selectionEnd = cursor
                refreshEditor()
            }
        }
    }

    override fun delete() {

        if (isShiftOn) {
            doc.replace(minSelection, maxSelection - minSelection, "")
            isShiftOn = false
            cursor = minSelection
            selectionStart = cursor
            selectionEnd = cursor
            refreshEditor()
        } else {
            if (cursor <= 0) return
            doc.replace(cursor - 1, 1, "")
            cursor = cursor - 1
            selectionStart = cursor
            selectionEnd = cursor
            refreshEditor()
        }
    }

    override fun deleteAfter() {
        if (cursor >= length) return
        doc.replace(cursor, 1, "")
        updateImmExtractedText()
        invalidate()
    }

    override fun undo() {

    }

    override fun redo() {

    }


    fun getSelectedText(): CharSequence? {
        val a = minSelection
        val b = maxSelection
        if (a == b || a < 0) return null
        return TextUtils.substring(text, a, b)
    }

}


class ActionTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // 默认样式
        setPadding(4.dp)
    }

    // 额外的辅助构造，方便在代码里直接用
    constructor(context: Context, text: String, block: () -> Unit) : this(context) {
        setText(text)
        setOnClickListener { block() }
    }
}


class GestureListener(val editor: CodeEditText) : GestureDetector.SimpleOnGestureListener() {

    var popup: PopupWindow? = null
    fun showActionMenu(e: MotionEvent) {
        editor.apply {
            // 如果 PopupWindow 已存在且正在显示，直接返回
            if (popup?.isShowing == true) {
                return
            }
            // 获取视图相对于窗口的位置
            val location = IntArray(2)
            editor.getLocationInWindow(location)

            // 获取点击位置相对于视图的坐标
            val viewX = e.x.toInt()
            val viewY = e.y.toInt()

            // 计算点击位置所在行号
            val line = min(lineCount - 1, (viewY / lineHeight))  // 基于 Y 坐标计算行号

            val actionViews = mutableListOf<TextView>()
            actionViews.add(ActionTextView(context, "全选") { selectAll() })
            actionViews.add(ActionTextView(context, "复制") { copy() })
            actionViews.add(ActionTextView(context, "剪切") { cut() })
            actionViews.add(ActionTextView(context, "粘贴") { paste() })

            // 获取菜单视图
            val view = LinearLayout(context)
            view.apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4.dp)
                setBackgroundColor(Color.WHITE)
            }
            actionViews.forEach {
                view.addView(it)
            }

            // 创建 PopupWindow
            popup = PopupWindow(view, -2, -2)

            // 确保view已布局完成，获取其宽度和高度
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val menuWidth = view.measuredWidth
            val menuHeight = view.measuredHeight
            // 计算该行的顶部位置
            val lineTop = line * lineHeight
            // 计算 PopupWindow 的 X 和 Y 坐标
            val xPos = (location[0] + viewX - menuWidth / 2).toInt() // 居中显示
            val yPos = location[1] + lineTop - menuHeight // 显示在该行的顶部
            // 显示 PopupWindow
            popup?.showAtLocation(editor, Gravity.NO_GRAVITY, xPos, yPos)
        }
    }


    override fun onDown(e: MotionEvent): Boolean {
        editor.apply {
            if (!scroller.isFinished) {
                scroller.abortAnimation()
            }
        }
        return true
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        editor.apply {
            popup?.dismiss()
            requestFocus()
            imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

            val x = e.x + scrollX - lineNumberPanelWidth
            val y = e.y + scrollY
            val line = min(lineCount - 1, (y / lineHeight).toInt())
            val lineStart = doc.getLineStart(line)
            val lineLength = doc.getRealLineLength(line)
            val s = doc.get(lineStart, lineLength)
            var column = 0
            if (x >= measureText(s)) {
                column = lineLength
            } else {
                // 找到列号（测量字符宽度）
                var currentWidth = 0f
                for (i in s.indices) {
                    val charWidth = measureText(s[i].toString())
                    if (currentWidth + charWidth / 2 >= x) {
                        break
                    }
                    currentWidth += charWidth
                    column++
                }
            }
            // 计算最终光标位置
            desiredColumn = column
            cursor = lineStart + column
            selectionStart = cursor
            selectionEnd = cursor
            if (isShiftOn) {
                isShiftOn = false
            }
            refreshEditor()
        }
        return true
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
    ): Boolean {
        editor.apply {
            val newX = (scrollX + distanceX.toInt()).coerceIn(0, maxScrollX)
            val newY = (scrollY + distanceY.toInt()).coerceIn(0, maxScrollY)
            scrollTo(newX, newY)
            postInvalidate()
        }
        return true
    }


    override fun onLongPress(e: MotionEvent) {
        super.onLongPress(e)
        showActionMenu(e)
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        showActionMenu(e)
        return true
    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        editor.apply {
            scroller.fling(
                scrollX,
                scrollY,
                (-velocityX).toInt(),
                (-velocityY).toInt(),
                0,
                maxScrollX,
                0,
                maxScrollY
            )
            postInvalidate()
        }
        return true
    }
}