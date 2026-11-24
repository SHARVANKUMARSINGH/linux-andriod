package com.linuxandroid.app

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R

/**
 * Reader Activity for displaying text content
 * Uses a WebView-based implementation with more flexibility
 */
class ReaderActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var titleText: TextView
    private var content: String = ""
    private var pageTitle: String = "Linux Android Reader"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_reader)

        webView = findViewById(R.id.webView)
        titleText = findViewById(R.id.titleText)

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        // Configure WebView
        configureWebView()

        // Load content from intent
        loadFromIntent()

        // Show content
        if (pageTitle.isNotEmpty()) {
            showContent()
        }
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            allowContentAccess = true
            allowFileAccess = true
        }
    }

    private fun loadFromIntent() {
        intent?.let {
            pageTitle = it.getStringExtra("TITLE") ?: "Linux Android"
            content = it.getStringExtra("CONTENT") ?: ""
        }

        titleText.text = pageTitle
    }

    private fun showContent() {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                        margin: 0;
                        padding: 20px;
                        background-color: #1e1e1e;
                        color: #ffffff;
                    }
                    .container {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    h1 {
                        color: #00e676;
                        border-bottom: 1px solid #333;
                        padding-bottom: 10px;
                    }
                    p {
                        line-height: 1.8;
                        color: #cccccc;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <h1>$pageTitle</h1>
                    <p>$content</p>
                </div>
            </body>
            </html>
        """.trimIndent()

        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }
}
