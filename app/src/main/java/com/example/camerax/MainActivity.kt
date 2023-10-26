package com.example.camerax

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraMetadata.FLASH_MODE_OFF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.camerax.ui.theme.CameraXTheme
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

                    FlashControlApp(uiState.collectAsState(),this){
                        uiState.value = it
                    }
                }
            }
        }
    }
}

@Composable
fun FlashControlApp(
    cameraState: State<CameraState>,
    lifecycleOwner: LifecycleOwner,
    setState: (CameraState) -> Unit
) {
    when (cameraState.value) {
        is CameraState.PermissionNotGranted -> {
            RequestPermission(setState)
        }

        is CameraState.Success -> {
            CameraScreen(lifecycleOwner)
        }
    }
}

@Composable
private fun CameraScreen(lifecycleOwner: LifecycleOwner) {
    val flashOn = remember { mutableStateOf(false) }
    val cameraControl = remember { mutableStateOf<CameraControl?>(null) }
    val cameraProvider by rememberUpdatedState(ProcessCameraProvider.getInstance(LocalContext.current))
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val preview = remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val previewView by remember { mutableStateOf(PreviewView(context)) }
    val cameraSelector = remember { mutableStateOf(CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()) }
    val provider = cameraProvider.get()
    DisposableEffect(Unit) {
        cameraProvider.addListener(Runnable {
            coroutineScope.launch(Dispatchers.Main) {
                preview.value = androidx.camera.core.Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val camera =
                    provider.bindToLifecycle(lifecycleOwner, cameraSelector.value, preview.value)
                cameraControl.value = camera.cameraControl
            }
        }, Executors.newSingleThreadExecutor())

        onDispose {
            cameraControl.value = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView }) {}
        Button(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    cameraControl.value?.enableTorch(flashOn.value)
                    flashOn.value = !flashOn.value
                }
            }
        ) {
            Text(if (flashOn.value) "Turn OFF" else "Turn ON")
        }

    }
}

@Composable
private fun RequestPermission(
    setState: (CameraState) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()){ granted ->
        if(granted){
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


