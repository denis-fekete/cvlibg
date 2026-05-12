# About

Computer Vision Library for Board Games, or CVLiBG is an end-to-end object detection workflow for developers creating
Android smartphone applications for recognizing game objects.

CVLiBG is an Android library written in Kotlin. Its purpose is to wrap camera control and object detection related
functionalities into simple, reusable, and modular components for developers to utilize. CVLiBG is built around CameraX
and OpenCV, with an additional module using the Open Neural Network Exchange Runtime for YOLO object detection models.
The detection pipeline is exposed through modular detector classes, allowing developers to use the provided solutions or
implement their own.

## Installing mobile library with Android projects

Computer Vision Library For Board Games is compiled with **JVM target 11** and **Kotlin 2.0.21**. Projects should use
Kotlin version **2.0.x** or newer compatible versions.

To include the library in the Android project, it is necessary to add the Jitpack repository. This is done inside the *
*settings.gradle.kts** file, in the project's root folder:

```
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Next, add the library dependency to the module's **build.gradle.kts** configuration file.

```
dependencies {
    implementation("com.github.denis-fekete:cvlibg:1.0.0")
    implementation("com.github.denis-fekete.cvlibg:cvlibg-yolo-onnx:1.0.0")
}
```

The **1.0.0** tag should be replaced with the desired version, which can be found at
[JitPack](https://jitpack.io/#denis-fekete/cvlibg)
