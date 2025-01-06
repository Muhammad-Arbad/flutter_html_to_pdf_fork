package com.afur.flutter_html_to_pdf

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.print.PdfPrinter
import android.print.PrintAttributes
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File

class HtmlToPdfConverter {

    interface Callback {
        fun onSuccess(filePath: String)
        fun onFailure(errorMessage: String)
        fun onTimeout()
    }

    private var retainedWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun convert(
        filePath: String,
        timeout: Int?,
        applicationContext: Context,
        callback: Callback
    ) {
        Log.d(TAG, "Starting conversion. Timeout: $timeout seconds")

        // Read HTML content
        val htmlFile = File(filePath)
        if (!htmlFile.exists()) {
            val error = "HTML file does not exist at $filePath"
            Log.e(TAG, error)
            callback.onFailure(error)
            return
        }
        val htmlContent = htmlFile.readText(Charsets.UTF_8)

        // Configure WebView
        val webView = WebView(applicationContext).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    Log.d(TAG, "WebView finished loading. URL: $url")
                    createPdfFromWebView(this@apply, applicationContext, callback)
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    val errorMessage = error?.description ?: "Unknown error"
                    Log.e(TAG, "WebView error: $errorMessage")
                    cleanupWebView()
                    callback.onFailure("WebView error: $errorMessage")
                }
            }

            webChromeClient = TimeoutWebChromeClient(timeout ?: DEFAULT_TIMEOUT_SECONDS, callback)
        }

        // Load the HTML content
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
        retainedWebView = webView
    }

    private fun createPdfFromWebView(
        webView: WebView,
        applicationContext: Context,
        callback: Callback
    ) {
        val path = applicationContext.filesDir
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            val printer = PdfPrinter(attributes)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val adapter = webView.createPrintDocumentAdapter(TEMP_DOCUMENT_NAME)
                printer.print(adapter, path, TEMP_FILE_NAME, object : PdfPrinter.Callback {
                    override fun onSuccess(filePath: String) {
                        Log.d(TAG, "PDF created successfully: $filePath")
                        callback.onSuccess(filePath)
                        cleanupWebView()
                    }

                    override fun onFailure() {
                        val error = "PDF generation failed."
                        Log.e(TAG, error)
                        callback.onFailure(error)
                        cleanupWebView()
                    }
                })
            } else {
                val error = "Unsupported Android version for PDF generation."
                Log.e(TAG, error)
                callback.onFailure(error)
                cleanupWebView()
            }
        } else {
            val error = "Unsupported Android version for PDF generation."
            Log.e(TAG, error)
            callback.onFailure(error)
            cleanupWebView()
        }
    }

    private fun cleanupWebView() {
        retainedWebView?.destroy()
        retainedWebView = null
        Log.d(TAG, "WebView resources released.")
    }

    companion object {
        private const val TAG = "HtmlToPdfConverter"
        private const val TEMP_DOCUMENT_NAME = "TemporaryDocumentName"
        private const val TEMP_FILE_NAME = "TemporaryDocumentFile.pdf"
        private const val DEFAULT_TIMEOUT_SECONDS = 10
    }

    private class TimeoutWebChromeClient(
        private val timeout: Int,
        private val callback: Callback
    ) : WebChromeClient() {

        private var progressHandler: Handler? = null
        private var progressRunnable: Runnable? = null
        private var countdown = timeout

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            Log.d(TAG, "WebView loading progress: $newProgress%")

            if (newProgress >= 100) {
                stopTimeout()
                return
            }

            if (progressHandler == null) {
                progressHandler = Handler(Looper.getMainLooper())
                progressRunnable = Runnable {
                    countdown--
                    if (countdown <= 0) {
                        Log.e(TAG, "Timeout reached. Triggering timeout callback.")
                        stopTimeout()
                        callback.onTimeout()
                    } else {
                        progressHandler?.postDelayed(progressRunnable!!, 1000)
                    }
                }
                progressHandler?.postDelayed(progressRunnable!!, 1000)
            }
        }

        private fun stopTimeout() {
            progressHandler?.removeCallbacks(progressRunnable!!)
            progressHandler = null
        }
    }
}
