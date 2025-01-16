package com.afur.flutter_html_to_pdf

import android.content.Context
import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

import android.util.Log

/** FlutterHtmlToPdfPlugin */
class FlutterHtmlToPdfPlugin: FlutterPlugin, MethodCallHandler {
  private lateinit var channel : MethodChannel
  private lateinit var applicationContext: Context

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_html_to_pdf")
    channel.setMethodCallHandler(this)

    applicationContext = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    if (call.method == "convertHtmlToPdf") {
      convertHtmlToPdf(call, result)
    } else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  private fun convertHtmlToPdf(call: MethodCall, result: Result) {
    val htmlFilePath = call.argument<String>("htmlFilePath")
    val timer = call.argument<Int>("timer")

    HtmlToPdfConverter().convert(htmlFilePath!!,timer, applicationContext, object : HtmlToPdfConverter.Callback {
      override fun onSuccess(filePath: String) {
        result.success(filePath)
      }

      override fun onFailure() {
        Log.e("TAG TAG TAG TAG", "onFailure onFailure onFailure onFailure onFailure")
        result.error("ERROR", "Unable to convert html to pdf document!", "")
      }

      override fun onRefreshTime() {
        Log.e("TAG", "onFailure onFailure onFailure onFailure onFailure")
        result.error("Refresh", "Unable to convert html to pdf document!", "")
      }
    })
  }
}

// package com.afur.flutter_html_to_pdf

// import android.content.Context
// import androidx.annotation.NonNull

// import io.flutter.embedding.engine.plugins.FlutterPlugin
// import io.flutter.plugin.common.MethodCall
// import io.flutter.plugin.common.MethodChannel
// import io.flutter.plugin.common.MethodChannel.MethodCallHandler
// import io.flutter.plugin.common.MethodChannel.Result

// /** FlutterHtmlToPdfPlugin */
// class FlutterHtmlToPdfPlugin: FlutterPlugin, MethodCallHandler {
//   private lateinit var channel : MethodChannel
//   private lateinit var applicationContext: Context

//   override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
//     channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_html_to_pdf")
//     channel.setMethodCallHandler(this)

//     applicationContext = flutterPluginBinding.applicationContext
//   }

//   override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
//     if (call.method == "convertHtmlToPdf") {
//       convertHtmlToPdf(call, result)
//     } else {
//       result.notImplemented()
//     }
//   }

//   override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
//     channel.setMethodCallHandler(null)
//   }

//   private fun convertHtmlToPdf(call: MethodCall, result: Result) {
//     val htmlFilePath = call.argument<String>("htmlFilePath")

//     HtmlToPdfConverter().convert(htmlFilePath!!, applicationContext, object : HtmlToPdfConverter.Callback {
//       override fun onSuccess(filePath: String) {
//         result.success(filePath)
//       }

//       override fun onFailure() {
//         result.error("ERROR", "Unable to convert html to pdf document!", "")
//       }
//     })
//   }
// }
