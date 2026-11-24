package com.linuxandroid.app

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R
import java.io.File

/**
 * File Editor Activity
 * Allows users to view and edit text files
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var filePathEditText: EditText
    private lateinit var browseButton: Button
    private lateinit var editorEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        filePathEditText = findViewById(R.id.filePathEditText)
        browseButton = findViewById(R.id.browseButton)
        editorEditText = findViewById(R.id.editorEditText)

        // Load file path from intent if provided
        intent?.getStringExtra("FILE_PATH")?.let { path ->
            filePathEditText.setText(path)
            loadFile(path)
        }

        browseButton.setOnClickListener {
            val path = filePathEditText.text.toString().trim()
            if (path.isNotEmpty()) {
                loadFile(path)
            } else {
                Toast.makeText(this, "Please enter a file path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFile(path: String) {
        try {
            val file = File(path)
            if (file.exists() && file.isFile) {
                val content = file.readText()
                editorEditText.setText(content)
            } else {
                Toast.makeText(this, "File not found: $path", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error loading file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
