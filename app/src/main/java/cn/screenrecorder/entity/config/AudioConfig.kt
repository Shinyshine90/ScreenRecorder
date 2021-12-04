package cn.screenrecorder.entity.config

import android.media.MediaRecorder


data class AudioConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val audioEncoder: Int = MediaRecorder.AudioEncoder.HE_AAC
)