package cn.screenrecorder.config

import android.media.MediaRecorder

enum class VideoSource(val source: Int) {
    CAMERA(MediaRecorder.VideoSource.CAMERA),
    SURFACE(MediaRecorder.VideoSource.SURFACE)
}

enum class VideoFormat(val format: Int) {
    THREE_GPP(MediaRecorder.OutputFormat.THREE_GPP),
    MPEG_4(MediaRecorder.OutputFormat.MPEG_4)
}

enum class VideoSize(val width: Int, val height: Int) {
    VD_1080P(1080, 1920),
    VD_720P(720, 1280),
    VD_640P(640, 960),
    VD_480P(480, 720),
    VD_360P(360, 640)
}

enum class VideoFrame(val frame: Int) {
    FRAME_15(15),
    FRAME_30(30),
    FRAME_45(45),
    FRAME_60(60)
}

enum class VideoEncoder(val encoder: Int) {
    ENCODER_H263(MediaRecorder.VideoEncoder.H263),
    ENCODER_H264(MediaRecorder.VideoEncoder.H264),
    ENCODER_MPEG_4_SP(MediaRecorder.VideoEncoder.MPEG_4_SP),
    ENCODER_VP8(MediaRecorder.VideoEncoder.VP8),
    ENCODER_HEVC(MediaRecorder.VideoEncoder.HEVC),

}

data class VideoConfig(
    val format: VideoFormat = VideoFormat.MPEG_4,
    val frame: VideoFrame = VideoFrame.FRAME_30,
    val size: VideoSize = VideoSize.VD_720P,
    val videoSource: VideoSource = VideoSource.SURFACE,
    val videoEncoder: VideoEncoder = VideoEncoder.ENCODER_H264
)
