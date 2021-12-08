package cn.screenrecorder

import android.content.Intent
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import cn.screenrecorder.databinding.ActivityMainBinding
import cn.screenrecorder.io.externalRecordDir
import cn.screenrecorder.io.outputFile
import cn.screenrecorder.media.mixture.RecordMixture

private const val TAG = "MainActivityKt"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(binding.navigation)
        }
        //startActivity(Intent(this, RecorderActivity::class.java))
        externalRecordDir()?.listFiles()?.forEach {
            Log.e(TAG, "onCreate: file ${it.path}")
        }
        //if (true) return
        val audioPath = "/storage/emulated/0/Android/data/cn.screenrecorder/files/records/2021-12-07-22-58-58-temp.aac"
        val videoPath = "/storage/emulated/0/Android/data/cn.screenrecorder/files/records/2021-12-07-22-58-58-temp.mp4"
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(videoPath)
        for (i in 0 until mediaExtractor.trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime.isNullOrEmpty()) continue
            when {
                mime.startsWith("video/") -> {
                    Log.e(TAG, "onCreate: " +
                            "width: ${trackFormat.getInteger(MediaFormat.KEY_WIDTH)} " +
                            "height: ${trackFormat.getInteger(MediaFormat.KEY_HEIGHT)}")
                }
                mime.startsWith("audio/") -> {
                    Log.e(TAG, "onCreate: " +
                            "channelCount: ${trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)} " +
                            "bitRate: ${trackFormat.getInteger(MediaFormat.KEY_BIT_RATE)} " +
                            "sampleRate: ${trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)}")
                }
            }
            Log.e(TAG, "onCreate: media mime:$mime")
        }

        RecordMixture(videoPath, audioPath, outputFile("assemble", "mp4").absolutePath).start()
    }

}