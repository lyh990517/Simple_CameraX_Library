package com.example.compose_camerax

import android.content.Context
import android.os.Environment
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXImpl : CameraX {

    private val _facing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    private val _flash = MutableStateFlow(false)

    private lateinit var previewView: PreviewView
    private lateinit var preview: Preview
    private lateinit var cameraProvider: ListenableFuture<ProcessCameraProvider>
    private lateinit var provider: ProcessCameraProvider
    private lateinit var imageCapture : ImageCapture
    private lateinit var camera: Camera
    private lateinit var context: Context
    private lateinit var executor : ExecutorService
    override fun initialize(context: Context) {
        previewView = PreviewView(context)
        preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        cameraProvider = ProcessCameraProvider.getInstance(context)
        provider = cameraProvider.get()
        imageCapture = ImageCapture.Builder().build()
        executor = Executors.newSingleThreadExecutor()
        this.context = context

    }

    override fun startCamera(
        lifecycleOwner: LifecycleOwner,
    ) {
        unBindCamera()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(_facing.value).build()
        cameraProvider.addListener({
            CoroutineScope(Dispatchers.Main).launch {
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            }
        },executor)
    }

    override fun takePicture(
        showMessage: (String) -> Unit
    ) {
        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/cameraX")
        if (!path.exists()) path.mkdirs();
        val photoFile = File(
            path, SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA
            ).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch() {
            imageCapture.takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(error: ImageCaptureException) {
                        error.printStackTrace()
                    }

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        showMessage(
                            "Capture Success!! Image Saved at  \n [${Environment.getExternalStorageDirectory().absolutePath}/${Environment.DIRECTORY_PICTURES}/cameraX]"
                        )
                    }
                })
        }
    }

    override fun flipCameraFacing() {
        if (_facing.value == CameraSelector.LENS_FACING_BACK) {
            _flash.value = false
            _facing.value = CameraSelector.LENS_FACING_FRONT
        } else {
            _facing.value = CameraSelector.LENS_FACING_BACK
        }
    }

    override fun turnOnOffFlash() {
        _flash.value = !_flash.value
        camera.cameraControl.enableTorch(_flash.value)
    }

    override fun unBindCamera() {
        provider.unbindAll()
    }

    override fun getPreviewView(): PreviewView = previewView
    override fun getFlashState(): StateFlow<Boolean> = _flash.asStateFlow()
    override fun getFacingState(): StateFlow<Int> = _facing.asStateFlow()

}