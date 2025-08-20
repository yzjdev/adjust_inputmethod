package com.example.myapplication

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    val TAG = "aaa"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        thread {
            var name = "test.txt"
            name = "TextView.java"
            val str = readText(assets.open(name))
            runOnUiThread {
                binding.editor.text = str
            }
        }

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            add(0, 0, 0, "切换输入法").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            add(0,1,0,"查看输入法").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            0 -> {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
            1 -> {
                val id = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                )
                Log.d(TAG, "softInput: $id")
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun readText(
        inputStream: InputStream,
        charsetName: String = "UTF-8",
        bufferSize: Int = 8192
    ): String {
        val sb = StringBuilder()
        inputStream.use { input ->
            BufferedReader(InputStreamReader(input, charsetName), bufferSize).use { reader ->
                val cbuf = CharArray(bufferSize)
                var charsRead: Int
                while (reader.read(cbuf).also { charsRead = it } != -1) {
                    sb.append(cbuf, 0, charsRead)
                }
            }
        }
        return sb.toString()
    }

}