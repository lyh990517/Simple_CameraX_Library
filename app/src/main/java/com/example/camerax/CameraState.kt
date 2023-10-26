package com.example.camerax

sealed class CameraState{
    object PermissionNotGranted : CameraState()

    object Success : CameraState()
}
