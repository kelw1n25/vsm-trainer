// swift-tools-version:5.10
import PackageDescription

// VSMCore — всё, кроме экранов: DTO, сеть, хранение токенов, кэш, репозитории, ViewModel.
// Зависит только от Foundation и Observation, поэтому тесты гоняются и на macOS, и в Linux-контейнере.
// VSMFeatures — экраны SwiftUI; собирается только там, где есть SwiftUI (Apple-платформы).
var targets: [Target] = [
    .target(name: "VSMCore"),
    .testTarget(name: "VSMCoreTests", dependencies: ["VSMCore"], resources: [.copy("Fixtures")]),
]
var products: [Product] = [.library(name: "VSMCore", targets: ["VSMCore"])]

#if canImport(SwiftUI)
targets.append(.target(name: "VSMFeatures", dependencies: ["VSMCore"]))
products.append(.library(name: "VSMFeatures", targets: ["VSMFeatures"]))
#endif

let package = Package(
    name: "VSMKit",
    defaultLocalization: "ru",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: products,
    targets: targets
)
