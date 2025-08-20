package com.github.yzjdev.editor

import org.eclipse.jface.text.Document

fun Document.getLineStart(line: Int) = getLineOffset(line)

fun Document.getLineEnd(line: Int) = getLineStart(line) + getRealLineLength(line)

fun Document.getLineText(line: Int) = get(getLineStart(line), getRealLineLength(line))
fun Document.getRealLineLength(line: Int) = getLineInformation(line).length
