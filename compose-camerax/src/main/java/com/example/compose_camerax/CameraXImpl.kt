package com.example.compose_camerax

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalLensFacing
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalLensFacing
class CameraXImpl : CameraX {

    private val _facing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    private val _flash = MutableStateFlow(false)
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    private val _recordingInfo = MutableSharedFlow<RecordingInfo>()
    private val _bitmapFlow = MutableSharedFlow<Bitmap>()

    private lateinit var previewView: PreviewView
    private lateinit var preview: Preview
    private lateinit var cameraProvider: ListenableFuture<ProcessCameraProvider>
    private lateinit var provider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var context: Context
    private lateinit var executor : ExecutorService
    private lateinit var recording: Recording
    private lateinit var mediaStoreOutput: MediaStoreOutputOptions

    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var imageCapture : ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    override fun initialize(context: Context) {
        previewView = PreviewView(context)
        preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        cameraProvider = ProcessCameraProvider.getInstance(context)
        provider = cameraProvider.get()
        imageCapture = ImageCapture.Builder().build()
        executor = Executors.newSingleThreadExecutor()
        this.context = context
        initializeVideo()
        initializeAnalyzer()

    }

    private fun initializeAnalyzer(){
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(executor) { imageProxy ->
            CoroutineScope(Dispatchers.Default).launch {
                val bitmap = imageProxy.toBitmap()
                val rotate = BitmapUtil.rotateBitmap(bitmap,90f)
                _bitmapFlow.emit(rotate!!)
                imageProxy.close()
            }
        }
    }
    @OptIn(ExperimentalCamera2Interop::class)
    fun initializeVideo(){
        val qualitySelector = QualitySelector.fromOrderedList(
            listOf(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD),
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))

        val recorder = Recorder.Builder()
            .setExecutor(executor).setQualitySelector(qualitySelector)
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val path =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/cameraX")
        if (!path.exists()) path.mkdirs();
        val name = "" + SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA
        ).format(System.currentTimeMillis()) + ".mp4"

        mediaStoreOutput = MediaStoreOutputOptions.Builder(context.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
            })
            .build()
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
                    imageCapture,
                    videoCapture,
                    imageAnalysis
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
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
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


    override fun startRecordVideo() {
     Log.e("record", "start")
        try {
            recording = videoCapture.output
                .prepareRecording(context, mediaStoreOutput)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(context)){
                    CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                        with(it.recordingStats){
                            _recordingInfo.emit(RecordingInfo(
                                duration = recordedDurationNanos,
                                sizeByte = numBytesRecorded,
                                audioAmplitude = audioStats.audioAmplitude
                            ))
                        }
                    }
                }
            _recordingState.value = RecordingState.OnRecord
        }catch (e : Exception){
            e.printStackTrace()
        }
    }

    override fun stopRecordVideo() {
        recording.stop()
        _recordingState.value = RecordingState.Idle
    }

    override fun resumeRecordVideo() {
        recording.resume()
        _recordingState.value = RecordingState.OnRecord
    }

    override fun pauseRecordVideo() {
        recording.pause()
        _recordingState.value = RecordingState.Paused
    }

    override fun closeRecordVideo() {
        Log.e("record","close")
        recording.close()
        _recordingState.value = RecordingState.Idle
    }

    override fun flipCameraFacing() {
        if (_facing.value == CameraSelector.LENS_FACING_BACK) {
            _flash.value = false
            _facing.value = CameraSelector.LENS_FACING_EXTERNAL
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
    override fun getRecordingState(): StateFlow<RecordingState> = _recordingState.asStateFlow()
    override fun getRecordingInfo(): SharedFlow<RecordingInfo> = _recordingInfo.asSharedFlow()
    override fun getImageBitmaps(): SharedFlow<Bitmap> = _bitmapFlow.asSharedFlow()
}