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
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.File
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError


class HtmlToPdfConverter {

    interface Callback {
        fun onSuccess(filePath: String)
        fun onFailure()
        fun onRefreshTime()
    }

    private var retainedWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun convert(filePath: String,timer:Int?, applicationContext: Context, callback: Callback) {
        Log.d(TAG, "Timer Value is: $timer")

        val webView = WebView(applicationContext)
        val htmlContent = File(filePath).readText(Charsets.UTF_8)
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.allowFileAccess = true

        webView.apply {
            var lastProgress = 0
            var progressCheckHandler: Handler? = null
            var progressRunnable: Runnable? = null

            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.allowFileAccess = true
            settings.domStorageEnabled = true

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {


                    super.onPageFinished(view, url)
                    Log.d(TAG, "WebView page finished loading. URL: $url")

                    progressCheckHandler?.removeCallbacks(progressRunnable!!)
                    Handler(Looper.getMainLooper()).postDelayed({
                        createPdfFromWebView(this@apply, applicationContext, callback)
                    }, 1000) // Add delay to ensure rendering is complete


                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "WebView error: ${error.description}")
                    callback.onFailure() // Trigger failure callback in case of error
                    cleanupWebView() // Cleanup WebView resources
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
                    super.onReceivedHttpError(view, request, errorResponse)
                    Log.e(TAG, "HTTP error: ${errorResponse.statusCode}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                private var progressTimerHandler: Handler? = null
                private var progressTimerRunnable: Runnable? = null
                private var countdownValue = timer!! // Initialize countdown variable
                private var valueOfProgress = 100 // Initialize progress variable

                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    Log.d(TAG, "WebView loading progress: $newProgress%")

                    // Reset countdownValue to 10 whenever this function is called
                    countdownValue = timer!!
                    valueOfProgress = newProgress

                    // Initialize the handler and runnable if not already set
                    if (progressTimerHandler == null) {
                        progressTimerHandler = Handler(Looper.getMainLooper())
                        progressTimerRunnable = object : Runnable {
                            override fun run() {
                                countdownValue--

                                Log.d(TAG, "Countdown value: $countdownValue")

                                if (countdownValue == 0 && valueOfProgress < 100) {

                                    webView.destroy()
                                    Log.e(TAG, "Countdown reached 0. Cleaning up resources and refreshing time. val = $valueOfProgress")

                                    progressTimerHandler?.removeCallbacks(this) // Stop the timer
                                    progressTimerHandler = null // Release the handler


                                    cleanupWebView() // Cleanup WebView resources
                                    callback.onRefreshTime()

                                } else if (valueOfProgress >= 100) {
                                    Log.d(TAG, "Progress complete. Stopping timer.")
                                    progressTimerHandler?.removeCallbacks(this) // Stop the timer
                                    progressTimerHandler = null // Release the handler
                                } else {

                                    Log.d(TAG, "Progress else. elseelseelseelseelseelseelseelseelseelseelseelse else.")

                                    // Schedule the next countdown decrement after 1 second
                                    progressTimerHandler?.postDelayed(this, 1000)
                                }
                            }
                        }
                        // Start the countdown timer
                        progressTimerHandler?.post(progressTimerRunnable!!)
                    }
                }
            }

        }

        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)

    }


    private fun createPdfFromWebView(webView: WebView, applicationContext: Context, callback: Callback) {
        val path = applicationContext.filesDir
        Log.d(TAG, "Creating PDF at path: $path")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

            Log.d(TAG, "PrintAttributes set for PDF generation.")

            val printer = PdfPrinter(attributes)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "Creating PrintDocumentAdapter.")
                val adapter = webView.createPrintDocumentAdapter(temporaryDocumentName)

                printer.print(adapter, path, temporaryFileName, object : PdfPrinter.Callback {
                    override fun onSuccess(filePath: String) {
                        Log.d(TAG, "PDF generation succeeded. File path: $filePath")
                        callback.onSuccess(filePath)
                        cleanupWebView()
                    }

                    override fun onFailure() {
                        Log.e(TAG, "PDF generation failed.")
                        callback.onFailure()
                        cleanupWebView()
                    }
                })
            } else {
                Log.e(TAG, "Unsupported Android version for PDF generation.")
                callback.onFailure()
                cleanupWebView()
            }
        } else {
            Log.e(TAG, "Unsupported Android version for PDF generation.")
            callback.onFailure()
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
        const val temporaryDocumentName = "TemporaryDocumentName"
        const val temporaryFileName = "TemporaryDocumentFile.pdf"
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
//     }

//     private var retainedWebView: WebView? = null

//     @SuppressLint("SetJavaScriptEnabled")
//     fun convert(filePath: String, applicationContext: Context, callback: Callback) {
//         Log.d(TAG, "Starting HTML to PDF conversion...")

//         val htmlFile = File(filePath)
//         if (!htmlFile.exists()) {
//             Log.e(TAG, "HTML file does not exist: $filePath")
//             callback.onFailure()
//             return
//         }

//         val htmlContent = htmlFile.readText(Charsets.UTF_8)
//         Log.d(TAG, "HTML content loaded from file: $filePath")

//         val webView = WebView(applicationContext).apply {
//             var lastProgress = 0
//             var progressCheckHandler: Handler? = null
//             var progressRunnable: Runnable? = null

//             settings.javaScriptEnabled = false
//             settings.javaScriptCanOpenWindowsAutomatically = false
//             settings.allowFileAccess = false
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
//                     callback.onFailure() // Trigger failure callback in case of error
//                     cleanupWebView() // Cleanup WebView resources
//                 }

//                 override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: android.webkit.WebResourceResponse) {
//                     super.onReceivedHttpError(view, request, errorResponse)
//                     Log.e(TAG, "HTTP error: ${errorResponse.statusCode}")
//                 }
//             }

//             webChromeClient = object : WebChromeClient() {
//                 override fun onProgressChanged(view: WebView?, newProgress: Int) {
//                     Log.d(TAG, "WebView loading progress: $newProgress%")
//                     if (progressCheckHandler == null) {
//                         progressCheckHandler = Handler(Looper.getMainLooper())
//                         progressRunnable = Runnable {

//                             Log.e(TAG, "Runnable Runnable Runnable")


//                             if (lastProgress == newProgress && newProgress < 100) {
//                                 Log.e(TAG, "WebView progress stuck at newProgress $newProgress%. Retrying...")
//                                 Log.e(TAG, "WebView progress stuck at lastProgress $lastProgress%. Retrying...")
// //                                reload() // Reload the WebView
//                             } else {
//                                 lastProgress = newProgress
//                                 progressCheckHandler?.postDelayed(progressRunnable!!, 5000)
//                             }
//                         }
//                         progressCheckHandler?.post(progressRunnable!!)
//                     }
//                 }
//             }
//         }


//         retainedWebView = webView
//         webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
//         Log.d(TAG, "WebView content loaded with base URL.")
//     }

//     private fun createPdfFromWebView(webView: WebView, applicationContext: Context, callback: Callback) {
//         val path = applicationContext.filesDir
//         Log.d(TAG, "Creating PDF at path: $path")

//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//             val attributes = PrintAttributes.Builder()
//                     .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
//                     .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
//                     .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

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



// // package com.afur.flutter_html_to_pdf

// // import android.annotation.SuppressLint
// // import android.content.Context
// // import android.os.Build
// // import android.print.PdfPrinter
// // import android.print.PrintAttributes
// // import android.webkit.WebView
// // import android.webkit.WebViewClient

// // import java.io.File


// // class HtmlToPdfConverter {

// //     interface Callback {
// //         fun onSuccess(filePath: String)
// //         fun onFailure()
// //     }

// //     @SuppressLint("SetJavaScriptEnabled")
// //     fun convert(filePath: String, applicationContext: Context, callback: Callback) {
// //         val webView = WebView(applicationContext)
// //         val htmlContent = File(filePath).readText(Charsets.UTF_8)
// //         webView.settings.javaScriptEnabled = true
// //         webView.settings.javaScriptCanOpenWindowsAutomatically = true
// //         webView.settings.allowFileAccess = true
// //         webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
// //         webView.webViewClient = object : WebViewClient() {
// //             override fun onPageFinished(view: WebView, url: String) {
// //                 super.onPageFinished(view, url)
// //                 createPdfFromWebView(webView, applicationContext, callback)
// //             }
// //         }
// //     }

// //     fun createPdfFromWebView(webView: WebView, applicationContext: Context, callback: Callback) {
// //         val path = applicationContext.filesDir
// //         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

// //             val attributes = PrintAttributes.Builder()
// //                 .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
// //                 .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
// //                 .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

// //             val printer = PdfPrinter(attributes)

// //             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
// //                 val adapter = webView.createPrintDocumentAdapter(temporaryDocumentName)

// //                 printer.print(adapter, path, temporaryFileName, object : PdfPrinter.Callback {
// //                     override fun onSuccess(filePath: String) {
// //                         callback.onSuccess(filePath)
// //                     }

// //                     override fun onFailure() {
// //                         callback.onFailure()
// //                     }
// //                 })
// //             }
// //         }
// //     }

// //     companion object {
// //         const val temporaryDocumentName = "TemporaryDocumentName"
// //         const val temporaryFileName = "TemporaryDocumentFile.pdf"
// //     }
// // }
