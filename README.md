# Android_CameraX_Sample
![camerax](https://github.com/lyh990517/Android_CameraX_Sample/assets/45873564/a96f2ccb-1583-40b2-b17f-a5b90ba0b5cd)

## Overview

This Module has been designed to simplify the usage of CameraX, making it extremely easy for users to work with. By providing minimal initialization on the user's part, this module facilitates straightforward integration of CameraX functionality into Android applications.

With this interface, users can effortlessly manage various camera-related tasks with minimal setup, ensuring a seamless and hassle-free experience.

## Methods

1. `initialize(context: Context)`: Initializes the CameraX module with the given application context.

2. `startCamera(lifecycleOwner: LifecycleOwner)`: Starts the camera with the provided `LifecycleOwner` to manage the camera's lifecycle.

3. `takePicture(showMessage: (String) -> Unit)`: Captures a still image and triggers the provided `showMessage` callback with the image result.

4. `startRecordVideo()`: Starts recording a video.

5. `stopRecordVideo()`: Stops the video recording.

6. `resumeRecordVideo()`: Resumes video recording if it was paused.

7. `pauseRecordVideo()`: Pauses video recording.

8. `closeRecordVideo()`: Closes the video recording session.

9. `flipCameraFacing()`: Toggles between the front and back camera facing.

10. `turnOnOffFlash()`: Toggles the camera flash on or off.

11. `unBindCamera()`: Unbinds and releases the camera resources.

12. `getPreviewView(): PreviewView`: Returns the `PreviewView` associated with the camera for displaying the camera preview.

13. `getFlashState(): StateFlow<Boolean>`: Provides a `StateFlow` that emits the current state of the camera flash (on or off).

14. `getFacingState(): StateFlow<Int>`: Provides a `StateFlow` that emits the current camera facing (front or back).

15. `getRecordingState(): StateFlow<RecordingState>`: Provides a `StateFlow` that emits the current video recording state (recording, paused, or stopped).

16. `getRecordingInfo(): SharedFlow<RecordingInfo>`: Provides a `SharedFlow` that emits information about the video recording, such as duration, file path, and other relevant details.

This CameraX Interface simplifies camera integration in your Android app, making it easier to manage and control camera functionality.



## Usage Examples
Here's an example of how to use CameraX

```kotlin
@Composable
fun Example() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraScope = rememberCoroutineScope()
    val cameraX = remember { CameraXFactory.create() }
    val previewView = remember { mutableStateOf<PreviewView?>(null) }
    val facing = cameraX.getFacingState().collectAsState()

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
    Button(
        onClick = { cameraX.takePicture {} }
    ) {
        Text("takePicture")
    }
}
```

## Customization
Feel free to customize CameraX Interface to suit your specific needs. Whether it's adjusting camera settings, tweaking the user interface, or tailoring the behavior, you have the flexibility to make it your own.

## Your Support Matters
If you find CameraX Interface helpful and it makes your development journey smoother, we kindly ask for your support. Please consider ⭐starring⭐ this repository—it motivates us to keep improving and providing you with valuable tools.
