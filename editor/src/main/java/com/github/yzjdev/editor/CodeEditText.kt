package com.github.yzjdev.editor

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
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedText
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Scroller
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.github.yzjdev.utils.dp
import com.github.yzjdev.utils.sp
import org.eclipse.jface.text.Document
import kotlin.math.max
import kotlin.math.min

class CodeEditText : View {
    val TAG = "aaa"

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    var doc = Document()
    val imm = context.getSystemService(InputMethodManager::class.java)
    private val inputConnection = CodeInputConnection(this)

    //光标与选区
    var cursor: Int = 0
    var selectionStart = cursor
    var selectionEnd = cursor

    var extractedTextRequest: ExtractedTextRequest? = null

    var isShiftOn: Boolean = false
    private var textMaxWidth = 0
    private val scroller = Scroller(context)
    private val gestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                requestFocus()
                imm.showSoftInput(this@CodeEditText, InputMethodManager.SHOW_IMPLICIT)

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
                cursor = lineStart + column
                selectionStart = cursor
                selectionEnd = cursor
                if (isShiftOn) {
                    isShiftOn = false
                }
                updateImm()
                scrollToVisible()
                invalidate()
                return true
            }

            override fun onScroll(
                e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
            ): Boolean {
                val newX = (scrollX + distanceX.toInt()).coerceIn(0, getMaxScrollX)
                val newY = (scrollY + distanceY.toInt()).coerceIn(0, getMaxScrollY)
                scrollTo(newX, newY)
                postInvalidate()
                return true
            }

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
            ): Boolean {
                scroller.fling(
                    scrollX,
                    scrollY,
                    (-velocityX).toInt(),
                    (-velocityY).toInt(),
                    0,
                    getMaxScrollX,
                    0,
                    getMaxScrollY
                )
                postInvalidate()
                return true
            }
        })

    private var baseTextSize = 20f.sp

    val lineNumberBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        alpha = 100
    }
    val textBackgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        alpha = 60
        style = Paint.Style.FILL_AND_STROKE
    }
    val lineNumberPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textAlign = Paint.Align.RIGHT
    }

    val currentLineBackgroundPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        alpha = 60
        style = Paint.Style.FILL_AND_STROKE
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
        alpha = 128
        strokeWidth = 2f.dp
        style = Paint.Style.FILL_AND_STROKE
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
        canvas.apply {

            // 绘制行号背景
            drawRect(
                0f,
                scrollY.toFloat(),
                lineNumberPanelWidth,
                (height + scrollY).toFloat(),
                lineNumberBackgroundPaint
            )

            drawRect(
                lineNumberPanelWidth,
                scrollY.toFloat(),
                (width + scrollX).toFloat(),
                (scrollY + height).toFloat(),
                textBackgroundPaint
            )
            if (length == 0) { // 处理文本为空时光标的绘制
                drawText(
                    "1",
                    lineNumberWidth + lineNumberPadding / 2,
                    -textPaint.fontMetrics.ascent,
                    lineNumberPaint
                )
                drawLine(
                    lineNumberPanelWidth,
                    0f,
                    lineNumberPanelWidth,
                    lineHeight,
                    cursorPaint
                )
                return
            }

            val visibleStartLine: Int = max(0, (scrollY / lineHeight).toInt())
            val visibleEndLine: Int = min(lineCount, ((scrollY + height) / lineHeight + 1).toInt())

            for (i in visibleStartLine until visibleEndLine) {
                val lineTextBaseY = i * lineHeight - textPaint.fontMetrics.ascent //文本基线

                // 绘制行号
                drawText(
                    "${i + 1}",
                    lineNumberWidth + lineNumberPadding / 2,
                    lineTextBaseY,
                    lineNumberPaint
                )

                val cursorLine = doc.getLineOfOffset(cursor)
                if (cursorLine == i && !isShiftOn) {
                    drawRect(
                        lineNumberPanelWidth,
                        cursorLine * lineHeight,
                        (width + scrollX).toFloat(),
                        (cursorLine + 1) * lineHeight,
                        currentLineBackgroundPaint
                    )
                }

                // 按行绘制文本

                val lineStart = doc.getLineStart(i)
                val lineLength = doc.getRealLineLength(i)
                val lineText = doc.get(lineStart, lineLength)

                val w = textPaint.measureText(lineText)
                if (w > textMaxWidth) textMaxWidth = w.toInt()
                drawText(lineText, lineNumberPanelWidth, lineTextBaseY, textPaint)

            }

            /**
             * 绘制光标
             */
            if (isShiftOn) {
                //选择状态
                val handleLeft = ContextCompat.getDrawable(
                    context, androidx.appcompat.R.drawable.abc_text_select_handle_left_mtrl
                )
                val handleRight = ContextCompat.getDrawable(
                    context, androidx.appcompat.R.drawable.abc_text_select_handle_right_mtrl
                )
                val handleMiddle = ContextCompat.getDrawable(
                    context, androidx.appcompat.R.drawable.abc_text_select_handle_middle_mtrl
                )
                //绘制选区
                val startLine = doc.getLineOfOffset(selectionStart)
                val endLine = doc.getLineOfOffset(selectionEnd)
                if (selectionStart == selectionEnd) {
                    val lineStart = doc.getLineOffset(startLine)
                    val x = lineNumberWidth + lineNumberPadding + measureText(
                        lineStart, selectionStart - lineStart
                    )
                    val y = startLine * lineHeight
                    drawLine(
                        x, y, x, y + lineHeight, selectionPaint
                    )


                    handleMiddle?.let {
                        val width = it.intrinsicWidth
                        val height = it.intrinsicHeight
                        it.setBounds(
                            (x - width / 2).toInt(),
                            (y + lineHeight).toInt(),
                            (x + width / 2).toInt(),
                            (y + lineHeight + height).toInt()
                        )
                        it.draw(canvas)
                    }

                    return
                }

                fun drawWater() {
                    val a = min(selectionStart, selectionEnd)
                    val b = max(selectionStart, selectionEnd)
                    val startLine = doc.getLineOfOffset(a)
                    val endLine = doc.getLineOfOffset(b)
                    val lineStartA = doc.getLineOffset(startLine)
                    val lineStartB = doc.getLineOffset(endLine)
                    var x = lineNumberWidth + lineNumberPadding + measureText(
                        lineStartA, a - lineStartA
                    )
                    var y = startLine * lineHeight
                    handleLeft?.let {
                        val width = it.intrinsicWidth
                        val height = it.intrinsicHeight
                        it.setBounds(
                            (x - width).toInt(),
                            (y + lineHeight).toInt(),
                            (x).toInt(),
                            (y + lineHeight + height).toInt()
                        )
                        it.draw(canvas)
                    }

                    x = lineNumberWidth + lineNumberPadding + measureText(
                        lineStartB, b - lineStartB
                    )
                    y = endLine * lineHeight
                    handleRight?.let {
                        val width = it.intrinsicWidth
                        val height = it.intrinsicHeight
                        it.setBounds(
                            (x).toInt(),
                            (y + lineHeight).toInt(),
                            (x + width).toInt(),
                            (y + lineHeight + height).toInt()
                        )
                        it.draw(canvas)
                    }
                }
                drawWater()
                if (startLine == endLine) {
                    val line = startLine
                    val lineStart = doc.getLineOffset(line)
                    val aw = lineNumberPanelWidth + measureText(
                        lineStart, selectionStart - lineStart
                    )
                    val ew = lineNumberPanelWidth + measureText(
                        lineStart, selectionEnd - lineStart
                    )
                    drawRect(aw, line * lineHeight, ew, (line + 1) * lineHeight, selectionPaint)
                } else {
                    if (startLine < endLine) {
                        drawRect(
                            lineNumberWidth + lineNumberPadding + measureText(
                                doc.getLineOffset(
                                    startLine
                                ), selectionStart - doc.getLineOffset(startLine)
                            ),
                            startLine * lineHeight,
                            (width + scrollX).toFloat(),
                            (startLine + 1) * lineHeight,
                            selectionPaint
                        )

                        for (i in startLine + 1 until endLine) {
                            drawRect(
                                lineNumberWidth + lineNumberPadding,
                                i * lineHeight,
                                (width + scrollX).toFloat(),
                                (i + 1) * lineHeight,
                                selectionPaint
                            )
                        }
                        drawRect(
                            lineNumberWidth + lineNumberPadding,
                            endLine * lineHeight,
                            lineNumberWidth + lineNumberPadding + measureText(
                                doc.getLineOffset(
                                    endLine
                                ), selectionEnd - doc.getLineOffset(endLine)
                            ),
                            (endLine + 1) * lineHeight,
                            selectionPaint
                        )
                    }

                    if (startLine > endLine) {
                        drawRect(
                            lineNumberWidth + lineNumberPadding,
                            startLine * lineHeight,
                            lineNumberWidth + lineNumberPadding + measureText(
                                doc.getLineOffset(
                                    startLine
                                ), selectionStart - doc.getLineOffset(startLine)
                            ),
                            (startLine + 1) * lineHeight,
                            selectionPaint
                        )

                        for (i in startLine - 1 downTo endLine + 1) {
                            drawRect(
                                lineNumberWidth + lineNumberPadding,
                                i * lineHeight,
                                (width + scrollX).toFloat(),
                                (i + 1) * lineHeight,
                                selectionPaint
                            )
                        }

                        drawRect(
                            lineNumberWidth + lineNumberPadding + measureText(
                                doc.getLineOffset(
                                    endLine
                                ), selectionEnd - doc.getLineOffset(endLine)
                            ),
                            endLine * lineHeight,
                            (width + scrollX).toFloat(),
                            (endLine + 1) * lineHeight,
                            selectionPaint
                        )
                    }
                }

            } else {
                //非选择状态
                if (cursor == 0) {
                    drawLine(
                        lineNumberPanelWidth,
                        0f,
                        lineNumberPanelWidth,
                        lineHeight,
                        cursorPaint
                    )
                } else {
                    val line = doc.getLineOfOffset(cursor)
                    val lineStart = doc.getLineStart(line)
                    val len = cursor - lineStart
                    val x = lineNumberPanelWidth + measureText(lineStart, len)
                    drawLine(x, line * lineHeight, x, (line + 1) * lineHeight, cursorPaint)
                }
            }
        }
    }

    //统一用textPaint测量字符宽度
    fun measureText(pos: Int, length: Int) = measureText(doc.get(pos, length))
    fun measureText(text: String) = textPaint.measureText(text)

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.initialSelStart = selectionStart
        outAttrs.initialSelEnd = selectionEnd
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT
        return inputConnection
    }

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
     * 一些get  set  实现
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

    private val lineNumberWidth: Float
        get() = measureText("$lineCount")

    var lineNumberPadding: Float = 16f.dp
        set(value) {
            field = value.coerceIn(16f.dp, 32f.dp)
            invalidate()
        }

    private val lineNumberPanelWidth: Float
        get() = lineNumberWidth + lineNumberPadding

    val lineHeight: Float
        get() = textPaint.fontMetrics.run {
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

    private val getMaxScrollX: Int
        get() = max(0, contentWidth - width / 2)
    private val getMaxScrollY: Int
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
        updateImm()
        scrollToVisible()
        invalidate()
    }

    fun clearMetaKeyStates(states: Int): Boolean {

        if (states == 193 && isShiftOn) {
            isShiftOn = false
            selectionStart = selectionEnd
            updateImm()
            scrollToVisible()
            invalidate()
            return true
        }
        return false
    }


    fun handleKeyDown(event: KeyEvent): Boolean {
        Log.d(TAG, "handleKeyDown: $event")

        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_MOVE_HOME,
            KeyEvent.KEYCODE_MOVE_END -> isShiftOn = event.isShiftPressed
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

    fun updateImm() {
        updateImmSelection()
        updateImmExtractedText()
    }

    fun updateImmSelection() {
        imm.updateSelection(this, selectionStart, selectionEnd, -1, -1)
    }


    fun updateImmExtractedText() {

        val et = ExtractedText()
        et.text = text
        et.selectionStart = selectionStart
        et.selectionEnd = selectionEnd
        if (isShiftOn) et.flags = et.flags or ExtractedText.FLAG_SELECTING
        imm.updateExtractedText(this, extractedTextRequest?.token ?: 0, et)

    }

    /**
     * 实现scrollToVisible
     */
    fun scrollToVisible() {
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
            targetX = (cursorX - H_MARGIN).toInt()
        } else if (cursorX > visibleRight - H_MARGIN) {
            targetX = (cursorX + H_MARGIN - width).toInt()
        }

        if (cursorTop < visibleTop) {
            targetY = cursorTop.toInt()
        } else if (cursorBottom > visibleBottom) {
            targetY = (cursorBottom - rect.height() + lineHeight).toInt()
        }


        if (targetX != scrollX || targetY != scrollY) {
            scroller.startScroll(
                scrollX,
                scrollY,
                targetX - scrollX,
                targetY - scrollY,
                200
            ) // 200ms 动画
            invalidate() // 触发 computeScroll()

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnGlobalLayoutListener {
            scrollToVisible()
        }
    }

    /**
     * 光标移动实现
     */
    enum class CursorAction {
        ACTION_LEFT, ACTION_RIGHT, ACTION_UP, ACTION_DOWN
    }

    fun moveCursor(action: CursorAction) {
        //防止光标越界
        when (action) {
            CursorAction.ACTION_LEFT, CursorAction.ACTION_UP -> if (cursor <= 0) return
            CursorAction.ACTION_RIGHT, CursorAction.ACTION_DOWN -> if (cursor >= length) return
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
        }
        cursor = newCursor
        if (isShiftOn) {
            selectionEnd = newCursor
        } else {
            selectionStart = cursor
            selectionEnd = cursor
        }
        updateImm()
        scrollToVisible()
        invalidate()

    }

    fun moveLeft() {
        moveCursor(CursorAction.ACTION_LEFT)
    }

    fun moveRight() {
        moveCursor(CursorAction.ACTION_RIGHT)
    }

    var desiredColumn = -1
    fun moveUp() {
        moveCursor(CursorAction.ACTION_UP)
    }

    fun moveDown() {
        moveCursor(CursorAction.ACTION_DOWN)
    }

    fun moveHome() {
        if (isShiftOn) {
            cursor = 0
            selectionEnd = 0
        } else {
            cursor = 0
            selectionStart = 0
            selectionEnd = 0
        }
        updateImm()
        scrollToVisible()
        invalidate()
    }

    fun moveEnd() {
        if (isShiftOn) {
            cursor = length
            selectionEnd = cursor
        } else {
            cursor = length
            selectionStart = cursor
            selectionEnd = cursor
        }
        updateImm()
        scrollToVisible()
        invalidate()
    }

    fun selectAll() {
        isShiftOn = true
        cursor = length
        selectionStart = 0
        selectionEnd = length
        updateImm()
        scrollToVisible()
        invalidate()
    }

    fun copy() {
        copy(getSelectedText())
    }

    fun copy(text: CharSequence?) {
        if (text == null) return
        try {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val clip = ClipData.newPlainText("", text)
            clipboard.setPrimaryClip(clip)
            isShiftOn = false
            selectionStart = cursor
            updateImm()
            invalidate()
            Toast.makeText(context, "已写入剪切板", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "字符太多了，无法写入剪切板", Toast.LENGTH_SHORT).show()
        }

    }

    fun cut() {
        val text = getSelectedText()
        if (text == null) return
        doc.replace(minSelection, maxSelection - minSelection, "")
        cursor = minSelection
        selectionStart = cursor
        selectionEnd = cursor

        copy(text)
        updateImm()
        scrollToVisible()
        invalidate()
    }

    fun paste() {
        if (isShiftOn && selectionStart != selectionEnd) {
            doc.replace(minSelection, maxSelection - minSelection, "")
            cursor = minSelection
            selectionStart = cursor
            selectionEnd = cursor
        }
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0).coerceToText(context).toString()
                // 在这里处理粘贴，例如插入到 EditText
                insert(text)
            }
        }
    }

    fun getSelectedText(): CharSequence? {
        val a = minSelection
        val b = maxSelection
        if (a == b || a < 0) return null
        return TextUtils.substring(text, a, b)
    }

    val minSelection: Int get() = min(selectionStart, selectionEnd)
    val maxSelection: Int get() = max(selectionStart, selectionEnd)

    /**
     * 文本插入
     * 如果是选择状态，删除选择文本，插入新的文本
     */
    fun insert(text: String) {
        if (isShiftOn) {
            val a = min(selectionStart, selectionEnd)
            val b = max(selectionStart, selectionEnd)
            if (a != b) doc.replace(a, b - a, "")
            cursor = a
            selectionStart = a
            selectionEnd = a
        }
        doc.replace(cursor, 0, text)
        cursor += text.length
        selectionStart = cursor
        selectionEnd = cursor
        updateImm()
        scrollToVisible()
        invalidate()
    }


    /**
     * 删除文本
     */
    fun delete() {
        if (length == 0) return

        // 计算删除区间
        val (delStart, delEnd) = run {
            val s = minSelection
            val e = maxSelection
            when {
                s != e -> s to e // 有选中
                s > 0 -> {
                    val line = doc.getLineOfOffset(s)
                    val lineStart = doc.getLineStart(line)
                    val prevLen =
                        if (s == lineStart && line > 0) doc.getLineDelimiter(line - 1).length else 1
                    (s - prevLen) to s
                }

                else -> return // 光标在最开始，无可删除内容
            }
        }

        doc.replace(delStart, delEnd - delStart, "")
        isShiftOn = false
        cursor = delStart
        selectionStart = cursor
        selectionEnd = cursor
        updateImm()
        scrollToVisible()
        invalidate()
    }
}

