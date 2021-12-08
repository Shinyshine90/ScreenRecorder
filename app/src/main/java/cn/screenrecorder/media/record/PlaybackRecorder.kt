package cn.screenrecorder.media.record

import android.media.*
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import cn.screenrecorder.utils.AccEncodeUtils
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 系统内音频录制,通过MediaCode硬编码PCM至Aac
 */
@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackRecorder(
    outputFileRetriever: () -> File,
    projectionRetriever: () -> MediaProjection
) : BaseRecorder(outputFileRetriever, projectionRetriever) {

    companion object {
        private const val TAG = "PlaybackRecorder"
    }

    @Volatile
    private var isRecording = false

    private lateinit var audioRecord: AudioRecord

    private lateinit var mediaCodec: MediaCodec

    private val executors: ExecutorService by lazy {
        Executors.newFixedThreadPool(2)
    }

    override fun prepare() {
        createAudioRecord()
        createAudioEncoder()
    }

    override fun start() {
        // 启动MediaCodec,等待传入数据
        mediaCodec.start()
        // 启动录制
        audioRecord.startRecording()
        // 更新状态
        isRecording = true
        executors.submit(::recordAudioTask)
        executors.submit(::encodeAudioTask)
    }

    override fun stop() {
        audioRecord.stop()
        mediaCodec.stop()
        isRecording = false
    }

    override fun release() {
        audioRecord.release()
        mediaCodec.release()
        executors.shutdown()
    }

    private fun createAudioRecord() {
        val audioFormat = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44_100)
            .build()
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projectionRetriever())
            .excludeUsage(AudioAttributes.USAGE_ASSISTANT)
            .build()
        val bufferSizeInBytes = AudioRecord.getMinBufferSize(
            44_100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(bufferSizeInBytes)
            .setAudioPlaybackCaptureConfig(captureConfig)
            .build()
        Log.e(TAG, "createAudioRecord: bufferSizeInBytes $bufferSizeInBytes")
        Log.e(TAG, "createAudioRecord: ${audioRecord.state == AudioRecord.STATE_INITIALIZED}")
    }

    private fun createAudioEncoder() {
        try {
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            val format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AAC, 44_100, 2)
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 30_000)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 96000)
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (!::mediaCodec.isInitialized) {
            Log.e(TAG, "create mediaEncode failed")
        }
    }

    private fun recordAudioTask() {
        //给MediaCode喂PCM数据
        val pcmDataBuffer = ByteArray(1024)
        while (isRecording) {
            val length = audioRecord.read(pcmDataBuffer, 0, pcmDataBuffer.size)
            if (length <= 0) continue
            val pcmChunk = ByteArray(length)
            System.arraycopy(pcmDataBuffer, 0, pcmChunk, 0, length)
            val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
            if (inputBufferIndex < 0) continue
            mediaCodec.getInputBuffer(inputBufferIndex)?.apply {
                clear()
                put(pcmChunk)
                limit(pcmChunk.size)
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, length, 0, MediaCodec.BUFFER_FLAG_KEY_FRAME)
            }
        }
    }

    private fun encodeAudioTask() {
        val fos = FileOutputStream(outputFileRetriever())
        val bufferInfo = MediaCodec.BufferInfo()
        while (isRecording) {
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, -1)
            //Log.e(TAG, "encodeAudioTask: outputBufferIndex $outputBufferIndex")
            if (outputBufferIndex < 0) continue
            Log.e(TAG, "encodeAudioTask: ${bufferInfo.flags} offset ${bufferInfo.offset} size ${bufferInfo.size}")
            mediaCodec.getOutputBuffer(outputBufferIndex)?.apply {
                position(bufferInfo.offset)
                limit(bufferInfo.offset + bufferInfo.size)
                val accChunk = ByteArray(7 + bufferInfo.size)
                AccEncodeUtils.addADTStoPacket(accChunk, accChunk.size)
                get(accChunk, 7, bufferInfo.size)
                position(bufferInfo.offset)
                fos.write(accChunk)
                Log.e(TAG, "encodeAudioTask: write acc data ${accChunk.size}")
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
            }
        }
        fos.close()
    }


}