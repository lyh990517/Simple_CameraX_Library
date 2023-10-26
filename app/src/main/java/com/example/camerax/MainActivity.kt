package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camerax.ui.theme.CameraXTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uiState = MutableStateFlow<CameraState>(CameraState.PermissionNotGranted)
        setContent {
            CameraXTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    FlashControlApp(uiState.collectAsState()){
                        uiState.value = it
                    }
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun FlashControlApp(
    cameraState: State<CameraState>,
    setState: (CameraState) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
    Scaffold(topBar = {}, snackbarHost = { SnackbarHost(snackBarHostState) }) {
        when (cameraState.value) {
            is CameraState.PermissionNotGranted -> {
                RequestPermission(setState)
            }

            is CameraState.Success -> {
                CameraScreen { message, label, duration ->
                    CoroutineScope(Dispatchers.Default).launch {
                        snackBarHostState.showSnackbar(
                            message, label, duration = duration
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScreen(showSnackBar: (String,String,SnackbarDuration) -> Unit) {
    val flashOn = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraControl = remember { mutableStateOf<CameraControl?>(null) }
    val cameraProvider by rememberUpdatedState(ProcessCameraProvider.getInstance(LocalContext.current))
    val cameraScope = rememberCoroutineScope()
    val context = LocalContext.current
    val preview = remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val previewView by remember { mutableStateOf(PreviewView(context)) }
    val cameraFacing = remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    val cameraSelector = CameraSelector.Builder().requireLensFacing(cameraFacing.value).build()
    val provider = cameraProvider.get()
    val imageCapture = remember { mutableStateOf(ImageCapture.Builder().build()) }
    DisposableEffect(cameraFacing.value) {
        cameraProvider.addListener({
            cameraScope.launch(Dispatchers.Main) {
                startCamera(
                    lifecycleOwner = lifecycleOwner,
                    preview = preview,
                    previewView = previewView,
                    cameraSelector = cameraSelector,
                    cameraProvider = provider,
                    cameraControl = cameraControl,
                    imageCapture = imageCapture.value
                )
            }
        }, Executors.newSingleThreadExecutor())

        onDispose {
            provider.unbindAll()
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView }) {
            imageCapture.value = ImageCapture.Builder()
                .build()
        }
        Button(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            onClick = {
                flashOn.value = !flashOn.value
                cameraControl.value?.enableTorch(flashOn.value)
            }
        ) {
            Text(if (flashOn.value) "Turn OFF" else "Turn ON")
        }
        Button(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp),
            onClick = {
                if (cameraFacing.value == CameraSelector.LENS_FACING_BACK) {
                    flashOn.value = false
                    cameraFacing.value = CameraSelector.LENS_FACING_FRONT
                } else {
                    cameraFacing.value = CameraSelector.LENS_FACING_BACK
                }
            }
        ) {
            Text(if (cameraFacing.value == CameraSelector.LENS_FACING_FRONT) "back" else "front")
        }

        Button(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            onClick = {
                takePicture(cameraScope,context, imageCapture.value, showSnackBar)
            }
        ) {
            Text("takePicture")
        }
    }
}

private fun startCamera(
    lifecycleOwner: LifecycleOwner,
    preview: MutableState<androidx.camera.core.Preview?>,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    cameraProvider: ProcessCameraProvider,
    cameraControl: MutableState<CameraControl?>,
    imageCapture: ImageCapture
) {
    cameraProvider.unbindAll()

    preview.value = androidx.camera.core.Preview.Builder()
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
    val camera = cameraProvider.bindToLifecycle(
        lifecycleOwner,
        cameraSelector,
        preview.value,
        imageCapture
    )
    cameraControl.value = camera.cameraControl
}
fun takePicture(
    cameraScope: CoroutineScope,
    context: Context,
    imageCapture: ImageCapture,
    showSnackBar: (String, String, SnackbarDuration) -> Unit
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
    cameraScope.launch(Dispatchers.Default + SupervisorJob()) {
        imageCapture.takePicture(outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(error: ImageCaptureException) {
                    error.printStackTrace()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    showSnackBar(
                        "Capture Success!! Image Saved at  \n [${Environment.getExternalStorageDirectory().absolutePath}/${Environment.DIRECTORY_PICTURES}/cameraX]",
                        "close",
                        SnackbarDuration.Short
                    )
                }
            })
    }
}


@Composable
private fun RequestPermission(
    setState: (CameraState) -> Unit
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setState(CameraState.Success)
            }
        }
    if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        LaunchedEffect(Unit) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    } else {
        setState(CameraState.Success)
    }
}


