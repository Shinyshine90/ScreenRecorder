package cn.screenrecorder.record

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

/**
 * 系统内音频录制
 */
@RequiresApi(Build.VERSION_CODES.Q)
class PlaybackRecorder(
    outputFileRetriever: () -> File,
    projectionRetriever: () -> MediaProjection
): BaseRecorder(outputFileRetriever, projectionRetriever) {

    companion object {
        private const val TAG = "PlaybackRecorder"
    }
    
    @Volatile
    private var isRecording = false
    
    private lateinit var audioRecord: AudioRecord

    private lateinit var recordThread: AudioRecordThread

    override fun prepare() {
        //TODO read config from where assembly
        val audioFormat = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44_100)
            .build()
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projectionRetriever())
            .excludeUsage(AudioAttributes.USAGE_ASSISTANT)
            .build()
        audioRecord = AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(
                AudioRecord.getMinBufferSize(
                44_100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            .setAudioPlaybackCaptureConfig(captureConfig)
            .build()
        recordThread = AudioRecordThread()
    }

    override fun start() {
        audioRecord.startRecording()
        isRecording = true
        recordThread.start()
    }

    override fun stop() {
        audioRecord.stop()
        isRecording = false
        recordThread.interrupt()
    }

    override fun release() {
        audioRecord.release()
        /*outputFileRetriever().apply {
            Log.e(TAG, "release length: ${this.length()}")
        }*/
    }
    
    inner class AudioRecordThread : Thread() {

        override fun run() {
            Log.e(TAG, "AudioRecordThread: start")
            while (isRecording && !isInterrupted) {
                val fos = FileOutputStream(outputFileRetriever())
                try {
                    val byteArray = ByteArray(1024)
                    var length: Int
                    while (audioRecord.read(byteArray ,0, 1024)
                            .also { length = it } != -1) {
                        Log.e(TAG, "AudioRecordThread: write $length")
                        fos.write(byteArray, 0 , length)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "AudioRecordThread: ${e.message}")
                } finally {
                    try {
                        fos.close()
                    } catch (e:Exception) {
                        Log.e(TAG, "AudioRecordThread: close stream error")
                    }
                }
            }
            Log.e(TAG, "AudioRecordThread: end")
        }

    }

}