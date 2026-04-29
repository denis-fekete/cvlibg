package com.fekete.cvlibg.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.OpenCVLoader

/**
 * Utility object/static class with common and shared utility focused functions.
 *
 * @author Denis Fekete <xfeket01@vutbr.cz>, <denis.fekete02@gmail.com>
 */
object CommonUtils {
    // must be unique to activity, therefore a "random" number was chosen
    private const val CAMERA_PERMISSION_REQUEST_CODE = 57243

    /**
     * Camera permission check, if the permission was not granted to activity, request it.
     * Camera permission must also be added to the projects `AndroidManifest.xml`:
     *
     * ```<uses-permission android:name = "android.permission.CAMERA"/>```
     */
    fun checkCameraPermission(context: Context, activity: Activity) {
        // Request camera access
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission was not granted, request it
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Setup OpenCV and print error on failure.
     */
    fun setupOpenCV() {
        if (OpenCVLoader.initLocal()) {
            Log.d("OpenCV", "OpenCV loaded successfully")
        } else {
            Log.e("OpenCV", "Failed to load OpenCV")
        }
    }
}