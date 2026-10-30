package com.example.ui.features.visual_lens

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

/** Reports device capability only; a positive result does not itself start an AR/depth session. */
object LensDepthCapabilities {
    fun describe(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return "Phone depth output needs Android 10+; ordinary camera preview remains available."
        }
        return try {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val supportsDepth = manager.cameraIdList.any { id ->
                val capabilities = manager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) == true
            }
            if (supportsDepth) "Depth-capable camera detected. Live depth/AR rendering still requires a compatible depth pipeline."
            else "No Camera2 depth-output capability detected. Standard camera and 3D lens effects are available."
        } catch (_: Exception) {
            "Camera depth capability could not be checked on this device."
        }
    }
}
