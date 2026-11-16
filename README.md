# WMTS Map

This Compose Multiplatform project contains two Gradle modules:

- `:wmts-map` is the reusable Compose UI library. It provides the `HelloWorld`
  composable.
- `:example` is a small app that uses the library. It has an Android launcher
  activity and exports an iOS framework with a Compose view controller.

The shared example screen displays **Hello world!** centered in a Material 3
surface.

## Requirements

- JDK 17 or newer
- Android SDK platform 36
- Xcode for building the iOS framework

## Build and test

```shell
./gradlew :example:assembleDebug
./gradlew :example:linkDebugFrameworkIosSimulatorArm64
./gradlew :wmts-map:allTests :example:allTests
```

The test tasks currently have no test sources. No formatter or static-analysis
tool is configured.

The iOS framework can be called from a SwiftUI host after linking the generated
`Example.framework`. The Kotlin entry point is
`ExampleMainViewControllerKt.MainViewController()`.
