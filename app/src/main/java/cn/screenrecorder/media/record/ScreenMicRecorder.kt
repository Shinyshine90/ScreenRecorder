package cn.screenrecorder.media.record

import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import java.io.File

/**
 * 屏幕 & 麦克风录制
 * MediaRecorder config in following order：
 * setAudioSource()
 * setVideoSource()
 * setOutputFormat()
 * setAudioEncoder()
 * setVideoEncoder()
 * setVideoSize()
 * setVideoFrameRate()
 * setVideoEncodingBitRate()
 * setOutputFile()
 * prepare()
 * start()
*/

class ScreenMicRecorder(
    outputFileRetriever: () -> File,
    projectionRetriever: () -> MediaProjection
): BaseRecorder(outputFileRetriever, projectionRetriever) {

    companion object {
        private const val TAG = "ScreenMicRecorder"
    }

    private lateinit var mediaRecorder: MediaRecorder

    private lateinit var virtualDisplay: VirtualDisplay

    private val screenWidth = 1080

    private val screenHeight = 2280

    private val virtualDpi = 5

    private val frameRate = 30
    
    override fun prepare() {
        mediaRecorder = createMediaRecorder().apply { prepare() }
        virtualDisplay = createVirtualDisplay()
    }

    override fun start() {
        mediaRecorder.start()
    }

    override fun stop() {
        mediaRecorder.stop()
    }

    override fun release() {
        mediaRecorder.release()
        virtualDisplay.release()
    }

    private fun createMediaRecorder(): MediaRecorder {
        //不要调整配置顺序
        val mediaRecorder = MediaRecorder()
        //
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        //
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        //
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        //
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
        //
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        //
        mediaRecorder.setVideoFrameRate(frameRate)
        //尺寸
        mediaRecorder.setVideoSize(screenWidth, screenHeight)
        //比特率
        mediaRecorder.setVideoEncodingBitRate(4_000_000)
        //设置输出路径
        mediaRecorder.setOutputFile(outputFileRetriever().absolutePath)
        return mediaRecorder
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return projectionRetriever().createVirtualDisplay(
            TAG, screenWidth, screenHeight, virtualDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.surface,
            null, null
        )
    }

}