// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CapacitorPluginPlugpag",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "CapacitorPluginPlugpag",
            targets: ["PlugPagPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "PlugPagPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/PlugPagPluginPlugin"),
        .testTarget(
            name: "PlugPagPluginPluginTests",
            dependencies: ["PlugPagPluginPlugin"],
            path: "ios/Tests/PlugPagPluginPluginTests")
    ]
)