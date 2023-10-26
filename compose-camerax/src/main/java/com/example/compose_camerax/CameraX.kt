package com.example.compose_camerax

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.StateFlow

interface CameraX {

    fun initialize(context: Context)
    fun startCamera(lifecycleOwner: LifecycleOwner)
    fun takePicture(showMessage: (String) -> Unit)
    fun startRecordVideo()
    fun stopRecordVideo()
    fun resumeRecordVideo()
    fun pauseRecordVideo()
    fun closeRecordVideo()
    fun flipCameraFacing()
    fun turnOnOffFlash()
    fun unBindCamera()
    fun getPreviewView() : PreviewView
    fun getFlashState() : StateFlow<Boolean>
    fun getFacingState() : StateFlow<Int>

}