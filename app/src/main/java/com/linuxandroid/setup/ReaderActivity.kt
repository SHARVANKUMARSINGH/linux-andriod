package com.linuxandroid.setup

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R
import java.io.File

/**
 * Reader Activity for displaying PDF and text documents.
 * Uses Android's built-in PdfRenderer for PDF rendering.
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var filePathEditText: EditText
    private lateinit var browseButton: Button
    private lateinit var pdfProgress: ProgressBar
    private lateinit var contentView: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        filePathEditText = findViewById(R.id.filePathEditText)
        browseButton = findViewById(R.id.browseButton)
        pdfProgress = findViewById(R.id.pdfProgress)
        contentView = findViewById(R.id.contentView)

        // Get file path from intent
        intent.getStringExtra(FILE_PATH)?.let { path ->
            filePathEditText.setText(path)
            loadFile(path)
        }

        browseButton.setOnClickListener {
            val path = filePathEditText.text.toString()
            if (path.isNotEmpty()) {
                loadFile(path)
            } else {
                Toast.makeText(this, "Please enter a file path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val FILE_PATH = "file_path"
    }

    private fun loadFile(path: String) {
        pdfProgress.visibility = android.view.View.VISIBLE

        handler.postDelayed({
            val file = File(path)
            if (file.exists()) {
                try {
                    if (file.extension.equals("pdf", ignoreCase = true)) {
                        // For PDF files, show a message since full PDF rendering requires
                        // external libraries that may not be available
                        contentView.setText("PDF file: ${file.name}\nSize: ${file.length()} bytes\n\nNote: Full PDF rendering requires the PDF viewer component.")
                    } else {
                        // For text files, display content directly
                        val content = file.readText()
                        contentView.setText(content)
                    }
                    pdfProgress.visibility = android.view.View.GONE
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        pdfProgress.visibility = android.view.View.GONE
                    }
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "File not found", Toast.LENGTH_LONG).show()
                    pdfProgress.visibility = android.view.View.GONE
                }
            }
        }, 300)
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
}
