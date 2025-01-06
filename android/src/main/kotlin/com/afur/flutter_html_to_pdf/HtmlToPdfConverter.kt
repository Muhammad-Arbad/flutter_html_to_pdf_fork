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


// package com.afur.flutter_html_to_pdf

// import android.annotation.SuppressLint
// import android.content.Context
// import android.os.Build
// import android.os.Handler
// import android.os.Looper
// import android.print.PdfPrinter
// import android.print.PrintAttributes
// import android.util.Log
// import android.webkit.WebChromeClient
// import android.webkit.WebView
// import android.webkit.WebViewClient
// import java.io.File
// import android.webkit.WebResourceRequest
// import android.webkit.WebResourceError


// class HtmlToPdfConverter {

//     interface Callback {
//         fun onSuccess(filePath: String)
//         fun onFailure()
//         fun onRefreshTime()
//     }

//     private var retainedWebView: WebView? = null

//     @SuppressLint("SetJavaScriptEnabled")
//     fun convert(filePath: String,timer:Int?, applicationContext: Context, callback: Callback) {
//         Log.d(TAG, "Timer Value is: $timer")

//         val webView = WebView(applicationContext)
//         val htmlContent = File(filePath).readText(Charsets.UTF_8)
//         webView.settings.javaScriptEnabled = true
//         webView.settings.javaScriptCanOpenWindowsAutomatically = true
//         webView.settings.allowFileAccess = true

//         webView.apply {
//             var lastProgress = 0
//             var progressCheckHandler: Handler? = null
//             var progressRunnable: Runnable? = null

//             settings.javaScriptEnabled = true
//             settings.javaScriptCanOpenWindowsAutomatically = true
//             settings.allowFileAccess = true
//             settings.domStorageEnabled = true

//             webViewClient = object : WebViewClient() {
//                 override fun onPageFinished(view: WebView, url: String) {


//                     super.onPageFinished(view, url)
//                     Log.d(TAG, "WebView page finished loading. URL: $url")

//                     progressCheckHandler?.removeCallbacks(progressRunnable!!)
//                     Handler(Looper.getMainLooper()).postDelayed({
//                         createPdfFromWebView(this@apply, applicationContext, callback)
//                     }, 1000) // Add delay to ensure rendering is complete


//                 }

//                 override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
//                     super.onReceivedError(view, request, error)
//                     Log.e(TAG, "WebView error: ${error.description}")
//                     cleanupWebView() // Cleanup WebView resources
//                     callback.onFailure() // Trigger failure callback in case of error
                    
//                 }

//                 override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
//                     super.onReceivedHttpError(view, request, errorResponse)
//                     Log.e(TAG, "HTTP error: ${errorResponse.statusCode}")
//                 }
//             }

//             webChromeClient = object : WebChromeClient() {
//                 private var progressTimerHandler: Handler? = null
//                 private var progressTimerRunnable: Runnable? = null
//                 private var countdownValue = timer!! // Initialize countdown variable
//                 private var valueOfProgress = 100 // Initialize progress variable

//                 override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                     Log.d(TAG, "WebView loading progress: $newProgress%")

//                     // Reset countdownValue to 10 whenever this function is called
//                     countdownValue = timer!!
//                     valueOfProgress = newProgress

//                     // Initialize the handler and runnable if not already set
//                     if (progressTimerHandler == null) {
//                         progressTimerHandler = Handler(Looper.getMainLooper())
//                         progressTimerRunnable = object : Runnable {
//                             override fun run() {
//                                 countdownValue--

//                                 Log.d(TAG, "Countdown value: $countdownValue")

//                                 if (countdownValue == 0 && valueOfProgress < 100) {

//                                     webView.destroy()
//                                     Log.e(TAG, "Countdown reached 0. Cleaning up resources and refreshing time. val = $valueOfProgress")

//                                     progressTimerHandler?.removeCallbacks(this) // Stop the timer
//                                     progressTimerHandler = null // Release the handler


//                                     cleanupWebView() // Cleanup WebView resources
//                                     callback.onRefreshTime()

//                                 } else if (valueOfProgress >= 100) {
//                                     Log.d(TAG, "Progress complete. Stopping timer.")
//                                     progressTimerHandler?.removeCallbacks(this) // Stop the timer
//                                     progressTimerHandler = null // Release the handler
//                                 } else {

//                                     Log.d(TAG, "Progress else. elseelseelseelseelseelseelseelseelseelseelseelse else.")

//                                     // Schedule the next countdown decrement after 1 second
//                                     progressTimerHandler?.postDelayed(this, 1000)
//                                 }
//                             }
//                         }
//                         // Start the countdown timer
//                         progressTimerHandler?.post(progressTimerRunnable!!)
//                     }
//                 }
//             }

//         }

//         webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

//     }


//     private fun createPdfFromWebView(webView: WebView, applicationContext: Context, callback: Callback) {
//         val path = applicationContext.filesDir
//         Log.d(TAG, "Creating PDF at path: $path")

//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//             val attributes = PrintAttributes.Builder()
//                 .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
//                 .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
//                 .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

//             Log.d(TAG, "PrintAttributes set for PDF generation.")

//             val printer = PdfPrinter(attributes)

//             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//                 Log.d(TAG, "Creating PrintDocumentAdapter.")
//                 val adapter = webView.createPrintDocumentAdapter(temporaryDocumentName)

//                 printer.print(adapter, path, temporaryFileName, object : PdfPrinter.Callback {
//                     override fun onSuccess(filePath: String) {
//                         Log.d(TAG, "PDF generation succeeded. File path: $filePath")
//                         callback.onSuccess(filePath)
//                         cleanupWebView()
//                     }

//                     override fun onFailure() {
//                         Log.e(TAG, "PDF generation failed.")
//                         callback.onFailure()
//                         cleanupWebView()
//                     }
//                 })
//             } else {
//                 Log.e(TAG, "Unsupported Android version for PDF generation.")
//                 callback.onFailure()
//                 cleanupWebView()
//             }
//         } else {
//             Log.e(TAG, "Unsupported Android version for PDF generation.")
//             callback.onFailure()
//             cleanupWebView()
//         }
//     }

//     private fun cleanupWebView() {
//         retainedWebView?.destroy()
//         retainedWebView = null
//         Log.d(TAG, "WebView resources released.")
//     }

//     companion object {
//         private const val TAG = "HtmlToPdfConverter"
//         const val temporaryDocumentName = "TemporaryDocumentName"
//         const val temporaryFileName = "TemporaryDocumentFile.pdf"
//     }
// }
