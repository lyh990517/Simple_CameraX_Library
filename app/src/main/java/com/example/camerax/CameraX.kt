package com.example.camerax

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope

interface CameraX {

    fun initialize(context: Context)
    fun startCamera(lifecycleOwner: LifecycleOwner, )

    fun takePicture(
        cameraScope: CoroutineScope,
        showSnackBar: (String, String, SnackbarDuration) -> Unit
    )

    fun flipCameraFacing()
    fun turnOnOffFlash()

    fun unBindCamera()

    fun getPreviewView() : PreviewView

}