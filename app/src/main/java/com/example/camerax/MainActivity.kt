package com.example.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camerax.ui.theme.CameraXTheme
import com.example.compose_camerax.CameraXFactory
import com.example.compose_camerax.RecordingInfo
import com.example.compose_camerax.RecordingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


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

                    FlashControlApp(uiState.collectAsState()) {
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
                CameraScreen { message ->
                    CoroutineScope(Dispatchers.Default).launch {
                        snackBarHostState.showSnackbar(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraScreen(showSnackBar: (String) -> Unit) {
    val flashOn = remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraScope = rememberCoroutineScope()
    val context = LocalContext.current
    val cameraX = remember { CameraXFactory.create() }
    val previewView = remember { mutableStateOf<PreviewView?>(null) }
    val facing = cameraX.getFacingState().collectAsState()
    val recordingState = cameraX.getRecordingState().collectAsState()
    val recordingInfo = cameraX.getRecordingInfo().collectAsState(RecordingInfo(0,0,0.0))
    LaunchedEffect(Unit) {
        cameraX.initialize(context = context)
        previewView.value = cameraX.getPreviewView()
    }
    DisposableEffect(facing.value) {
        cameraScope.launch(Dispatchers.Main) {
            cameraX.startCamera(lifecycleOwner = lifecycleOwner)
        }
        onDispose {
            cameraX.unBindCamera()
        }
    }
    Box(Modifier.fillMaxSize()) {
        previewView.value?.let { preview -> AndroidView(modifier = Modifier.fillMaxSize(), factory = { preview }) {} }
        Row(
            Modifier
                .height(100.dp)
                .align(Alignment.TopStart)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)) {
                Text("${(recordingInfo.value.duration / 1000000000.0) } second", fontSize = 18.sp, color = Color.White)
                Text("${recordingInfo.value.sizeByte / 1000000.0} Mbyte", fontSize = 18.sp, color = Color.White)
            }
            Row(
                Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.Bottom) {
                repeat(25){
                    Box(modifier = Modifier.size(4.dp,(recordingInfo.value.audioAmplitude * it * 4).dp).background(Color.Black))
                }
                repeat(25){
                    Box(modifier = Modifier.size(4.dp,((recordingInfo.value.audioAmplitude * (100 - it * 4))).dp).background(Color.Black))
                }
            }
        }
        Column(Modifier.align(Alignment.BottomCenter)) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                when(recordingState.value){
                    is RecordingState.OnRecord -> {
                        Button(
                            modifier = Modifier
                                .padding(10.dp),
                            onClick = {
                                cameraX.closeRecordVideo()
                            }
                        ) {
                            Text("close")
                        }
                        Button(
                            modifier = Modifier
                                .padding(10.dp),
                            onClick = {
                                cameraX.stopRecordVideo()
                            }
                        ) {
                            Text("stop")
                        }
                        Button(
                            modifier = Modifier
                                .padding(10.dp),
                            onClick = {
                                cameraX.pauseRecordVideo()
                            }
                        ) {
                            Text("pause")
                        }
                    }
                    is RecordingState.Idle -> {
                        Button(
                            modifier = Modifier
                                .padding(10.dp),
                            onClick = {
                                cameraX.startRecordVideo()
                            }
                        ) {
                            Text("record")
                        }
                    }
                    is RecordingState.Paused -> {
                        Button(
                            modifier = Modifier
                                .padding(10.dp),
                            onClick = {
                                cameraX.resumeRecordVideo()
                            }
                        ) {
                            Text("resume")
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    modifier = Modifier
                        .padding(10.dp),
                    onClick = {
                        cameraX.turnOnOffFlash()
                    }
                ) {
                    Text(if (flashOn.value) "Turn OFF" else "Turn ON")
                }
                Button(
                    modifier = Modifier
                        .padding(10.dp),
                    onClick = {
                        cameraX.flipCameraFacing()
                    }
                ) {
                    Text(if (facing.value == CameraSelector.LENS_FACING_FRONT) "back" else "front")
                }

                Button(
                    modifier = Modifier
                        .padding(10.dp),
                    onClick = {
                        cameraX.takePicture(showSnackBar)
                    }
                ) {
                    Text("takePicture")
                }
            }
        }

    }
}


@Composable
private fun RequestPermission(
    setState: (CameraState) -> Unit
) {
    val context = LocalContext.current
    val audioLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                setState(CameraState.Success)
            }
        }
    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
        LaunchedEffect(Unit) {
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }
    } else {
        setState(CameraState.Success)
    }
}


