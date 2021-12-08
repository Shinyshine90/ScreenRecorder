package cn.screenrecorder.media.mixture

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.lang.IllegalArgumentException
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 将录制的音视频文件合成
 * MediaMuxer 多轨道合成音频
 */

private const val TAG = "RecordMixture"

class RecordMixture(
    private val inputVideoPath: String,
    private val inputAudioPath: String,
    private val outputVideoPath: String
) {

    private val videoExtractor: MediaExtractor = MediaExtractor().apply {
        setDataSource(inputVideoPath)
    }

    private val audioExtractor: MediaExtractor = MediaExtractor().apply {
        setDataSource(inputAudioPath)
    }

    private var mediaMuxer = MediaMuxer(outputVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

    private var executorService: ExecutorService = Executors.newSingleThreadExecutor()

    fun start() {
        executorService.submit(::startMixture)
    }

    @SuppressLint("WrongConstant")
    private fun startMixture() {
        Log.e(TAG, "startMixture: start")
        var vVideoTrackIndexInExtractor: Int = -1
        var vAudioTrackIndexInExtractor: Int = -1
        var aAudioTrackIndexInExtractor: Int = -1

        var vVideoTrackTargetIndex: Int = -1
        var vAudioTrackTargetIndex: Int = -1
        var aAudioTrackTargetIndex: Int = -1

        for (i in 0 until videoExtractor.trackCount) {
            val trackFormat = videoExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime.isNullOrEmpty()) continue
            when {
                mime.startsWith("video/") -> {
                    vVideoTrackIndexInExtractor = i
                    vVideoTrackTargetIndex = mediaMuxer.addTrack(trackFormat)
                    Log.e(TAG, "startMixture: add video track from video, track index $vVideoTrackTargetIndex ")
                }
                mime.startsWith("audio/") -> {
                    vAudioTrackIndexInExtractor = i
                    vAudioTrackTargetIndex = mediaMuxer.addTrack(trackFormat)
                    Log.e(TAG, "startMixture: add audio track from video, track index $vAudioTrackTargetIndex")
                }
            }
        }

        for (i in 0 until audioExtractor.trackCount) {
            val trackFormat = audioExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime.isNullOrEmpty()) continue
            when {
                mime.startsWith("audio/") -> {
                    aAudioTrackIndexInExtractor = i
                    aAudioTrackTargetIndex = mediaMuxer.addTrack(trackFormat)
                    Log.e(TAG, "startMixture: add audio track from audio, track index $aAudioTrackTargetIndex")
                }
            }
        }

        if (vVideoTrackIndexInExtractor < 0) {
            throw IllegalArgumentException("invalidate input video path, parse error")
        }

        mediaMuxer.start()

        val byteBuffer = ByteBuffer.allocate(512 * 1024)
        val bufferInfo = MediaCodec.BufferInfo()
        var sampleSize:Int

        videoExtractor.selectTrack(vVideoTrackIndexInExtractor)
        while (videoExtractor.readSampleData(byteBuffer, 0).apply { sampleSize = this } > 0) {
            bufferInfo.flags = videoExtractor.sampleFlags
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = videoExtractor.sampleTime
            mediaMuxer.writeSampleData(vVideoTrackTargetIndex, byteBuffer, bufferInfo)
            videoExtractor.advance()
        }

        videoExtractor.selectTrack(vAudioTrackIndexInExtractor)
        while (videoExtractor.readSampleData(byteBuffer, 0).apply { sampleSize = this } > 0) {
            bufferInfo.flags = videoExtractor.sampleFlags
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = videoExtractor.sampleTime
            mediaMuxer.writeSampleData(vAudioTrackTargetIndex, byteBuffer, bufferInfo)
            videoExtractor.advance()
        }

        audioExtractor.selectTrack(aAudioTrackIndexInExtractor)
        while (audioExtractor.readSampleData(byteBuffer, 0).apply { sampleSize = this } > 0) {
            Log.e(TAG, "startMixture: readSample audio $sampleSize" )
            bufferInfo.flags = audioExtractor.sampleFlags
            bufferInfo.offset = 0
            bufferInfo.size = sampleSize
            bufferInfo.presentationTimeUs = audioExtractor.sampleTime
            mediaMuxer.writeSampleData(aAudioTrackTargetIndex, byteBuffer, bufferInfo)
            audioExtractor.advance()
        }

        videoExtractor.release()
        audioExtractor.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        Log.e(TAG, "finish: " )
    }

}