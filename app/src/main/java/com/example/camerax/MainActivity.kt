package com.example.camerax

import android.hardware.camera2.CameraMetadata.FLASH_MODE_OFF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    FlashControlApp(this)
                }
            }
        }
    }
}

@Composable
fun FlashControlApp(lifecycleOwner: LifecycleOwner) {
    var flashOn by remember { mutableStateOf(false) }
    val cameraControl = remember { mutableStateOf<CameraControl?>(null) }
    val cameraProvider =
        rememberUpdatedState(ProcessCameraProvider.getInstance(LocalContext.current))
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val preview = remember { mutableStateOf<androidx.camera.core.Preview?>(null) }
    val previewView by remember { mutableStateOf(PreviewView(context)) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = cameraProvider.value
        cameraProviderFuture.addListener(Runnable {
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            val provider = cameraProvider.value.get()
            coroutineScope.launch {
                withContext(Dispatchers.Main) {

                    preview.value = androidx.camera.core.Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    val camera =
                        provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview.value)
                    cameraControl.value = camera.cameraControl

                }
            }
        }, Executors.newSingleThreadExecutor())

        onDispose {
            cameraControl.value = null
        }
    }
    Box(Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView }) {

        }
        Button(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp),
            onClick = {
                coroutineScope.launch(Dispatchers.Default) {
                    cameraControl.value?.enableTorch(!flashOn)
                    flashOn = !flashOn
                }
            }
        ) {
            Text(if (flashOn) "Turn OFF" else "Turn ON")
        }

    }
}

