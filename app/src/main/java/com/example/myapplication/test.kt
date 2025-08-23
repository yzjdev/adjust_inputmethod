package com.example.myapplication

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import kotlin.random.Random

fun main() {

    val file = File("""E:\Projects\as\myprojects\app\src\main\assets\test2.txt""")

    val targetLength = 2000000


    // 候选字符集：a-z A-Z 0-9
    val candidates = buildList<Char> {
        addAll('a'..'z')
        addAll('A'..'Z')
        addAll('0'..'9')
    }

    BufferedWriter(FileWriter(file)).use { writer ->
        var written = 0
        while (written < targetLength) {
            if (Random.nextInt(100) < 2 && written < targetLength - 1) {
                // 2% 概率插入换行
                writer.write('\n'.code)
                written++
            } else {
                val c = candidates.random()
                writer.write(c.code)
                written++
            }
        }
    }

    println("文件已生成: ${file.absolutePath}, 大小=${file.length()} 字节")
}