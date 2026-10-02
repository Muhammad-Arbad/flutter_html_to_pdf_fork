// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "flutter_html_to_pdf",
    platforms: [
        .iOS("13.0")
    ],
    products: [
        .library(name: "flutter-html-to-pdf", targets: ["flutter_html_to_pdf"])
    ],
    dependencies: [],
    targets: [
        .target(
            name: "flutter_html_to_pdf",
            dependencies: [],
            resources: []
        )
    ]
)
