package com.example.compose_camerax

sealed class RecordingState {
    object Idle : RecordingState()
    object OnRecord : RecordingState()
    object Paused : RecordingState()

}
