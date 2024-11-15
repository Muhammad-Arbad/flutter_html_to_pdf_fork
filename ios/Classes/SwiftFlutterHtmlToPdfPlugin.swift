import Flutter
import UIKit
import WebKit

public class SwiftFlutterHtmlToPdfPlugin: NSObject, FlutterPlugin {
    var wkWebView: WKWebView!
    var urlObservation: NSKeyValueObservation?

    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_html_to_pdf", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterHtmlToPdfPlugin()
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "convertHtmlToPdf":
            let args = call.arguments as? [String: Any]
            let htmlFilePath = args?["htmlFilePath"] as? String

            guard let htmlFilePath = htmlFilePath else {
                result(FlutterError(code: "INVALID_ARGUMENT", message: "htmlFilePath is required", details: nil))
                return
            }

            // Create a hidden WKWebView
            let viewController = UIApplication.shared.delegate?.window??.rootViewController
            wkWebView = WKWebView(frame: viewController!.view.bounds)
            wkWebView.isHidden = true
            wkWebView.tag = 100
            viewController?.view.addSubview(wkWebView)

            // Load HTML content
            let htmlFileContent = FileHelper.getContent(from: htmlFilePath)
            wkWebView.loadHTMLString(htmlFileContent, baseURL: Bundle.main.bundleURL)

            // Observe loading completion
            urlObservation = wkWebView.observe(\.isLoading, changeHandler: { [weak self] webView, _ in
                guard let self = self else { return }
                if !webView.isLoading {
                    DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                        // Generate PDF
                        if let pdfFilePath = self.generatePDF(for: webView) {
                            result(pdfFilePath)
                        } else {
                            result(FlutterError(code: "PDF_GENERATION_FAILED", message: "Failed to generate PDF", details: nil))
                        }

                        // Cleanup
                        self.cleanupWebView(viewController: viewController)
                    }
                }
            })

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    private func generatePDF(for webView: WKWebView) -> String? {
        // A4 size in points: 595.2 x 841.8
        let pageFrame = CGRect(x: 0, y: 0, width: 595.2, height: 841.8)
        let renderer = UIPrintPageRenderer()
        renderer.setValue(pageFrame, forKey: "paperRect")
        renderer.setValue(pageFrame, forKey: "printableRect")

        // Add print formatter
        renderer.addPrintFormatter(webView.viewPrintFormatter(), startingAtPageAt: 0)

        // Create PDF context
        let pdfData = NSMutableData()
        UIGraphicsBeginPDFContextToData(pdfData, pageFrame, nil)
        for i in 0..<renderer.numberOfPages {
            UIGraphicsBeginPDFPage()
            renderer.drawPage(at: i, in: UIGraphicsGetPDFContextBounds())
        }
        UIGraphicsEndPDFContext()

        // Save PDF to temporary directory
        let tempDirectory = NSTemporaryDirectory()
        let pdfFilePath = tempDirectory.appending("generated.pdf")
        pdfData.write(toFile: pdfFilePath, atomically: true)

        return pdfFilePath
    }

    private func cleanupWebView(viewController: UIViewController?) {
        if let viewWithTag = viewController?.view.viewWithTag(100) {
            viewWithTag.removeFromSuperview()
        }

        // Clear WKWebView cache
        if #available(iOS 9.0, *) {
            WKWebsiteDataStore.default().fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes()) { records in
                records.forEach { record in
                    WKWebsiteDataStore.default().removeData(ofTypes: record.dataTypes, for: [record], completionHandler: {})
                }
            }
        }

        // Dispose of WKWebView
        urlObservation = nil
        wkWebView = nil
    }
}
