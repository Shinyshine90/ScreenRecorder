package cn.screenrecorder

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import android.os.Build
import android.app.*

import android.graphics.BitmapFactory
import android.hardware.display.VirtualDisplay
import android.media.*
import java.io.FileOutputStream
import kotlin.concurrent.thread

/**
setAudioSource()
setVideoSource()
setOutputFormat()
setAudioEncoder()
setVideoEncoder()
setVideoSize()
setVideoFrameRate()
setVideoEncodingBitRate()
setOutputFile()
prepare()
start()
 */
class RecordService : Service() {

    companion object {

        private const val TAG = "ScreenRecordService"

        private const val PARAM_CODE = "result_code"

        private const val PARAM_PROJECTION_TOKEN = "media_projection_token"

        fun create(ctx: Context, resultCode: Int, token: Intent) =
            Intent(ctx, RecordService::class.java).apply {
                putExtra(PARAM_CODE, resultCode)
                putExtra(PARAM_PROJECTION_TOKEN, token)
            }
    }

    private val screenWidth = 1080

    private val screenHeight = 2280

    private val virtualDpi = 5

    private val frameRate = 30

    private var originIntent = Intent()

    private var mediaProjection: MediaProjection? = null

    private var mediaRecorder: MediaRecorder? = null

    private var audioRecord: AudioRecord? = null

    private var virtualDisplay: VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand: $startId")
        createNotificationChannel()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        Log.e(TAG, "onBind: ")
        originIntent = intent
        return ScreenRecordBinder(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e(TAG, "onUnbind: ")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy: ")
        super.onDestroy()
        stopForeground(true)
    }

    fun startRecord() {
        mediaProjection = createProjection() ?: return
        mediaRecorder = createMediaRecorder()
        audioRecord = createAudioRecorder()
        virtualDisplay = setupVirtualDisplay()
        mediaRecorder?.start()
        audioRecord?.startRecording()
        startRecordPcm()
    }

    fun releaseRecord() {
        stopForeground(true)
        audioRecord?.stop()
        audioRecord?.release()
        mediaRecorder?.stop()
        mediaRecorder?.release()
        virtualDisplay?.release()
        mediaProjection?.stop()
        audioRecord = null
        mediaRecorder = null
        mediaProjection = null
        virtualDisplay = null
    }

    private fun createProjection(): MediaProjection? {
        val resultCode = originIntent.getIntExtra(PARAM_CODE, Activity.RESULT_OK)
        val projectionToken = originIntent.getParcelableExtra<Intent>(PARAM_PROJECTION_TOKEN)
        return projectionToken?.run {
            val protection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                .getMediaProjection(resultCode, this)
            protection
        }
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
        mediaRecorder.setOutputFile(getOutputFile("mp4"))
        try {
            mediaRecorder.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaRecorder
    }

    private fun createAudioRecorder(): AudioRecord? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return null
        }
        val projection = mediaProjection ?: return null
        val audioFormat = AudioFormat.Builder()
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44_100)
            .build()
        val captureConfig = AudioPlaybackCaptureConfiguration.Builder(projection)
            .excludeUsage(AudioAttributes.USAGE_ASSISTANT)
            .build()
        return AudioRecord.Builder()
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(AudioRecord.getMinBufferSize(
                44_100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            .setAudioPlaybackCaptureConfig(captureConfig)
            .build()
    }

    private fun setupVirtualDisplay(): VirtualDisplay? {
        val projection = mediaProjection
        val recorder = mediaRecorder
        return if (projection != null && recorder != null) {
            projection.createVirtualDisplay(
                TAG, screenWidth, screenHeight, virtualDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, recorder.surface, null, null
            )
        } else null
    }

    class ScreenRecordBinder(val service: RecordService) : Binder()

    private fun startRecordPcm() {
        val audioRecord = audioRecord ?: return
        Thread {
            Log.e(TAG, "createAudioRecorder: start", )
            try {
                val fos = FileOutputStream(getOutputFile("pcm"))
                val byteArray = ByteArray(1024)
                var length: Int
                while (audioRecord.read(byteArray ,0, 1024).apply { length = this } > 0) {
                    Log.e(TAG, "createAudioRecorder: write $length")
                    fos.write(byteArray, 0 , length)
                }
            } catch (e:Exception) {
                Log.e(TAG, "createAudioRecorder: ${e.message}")
            }
            Log.e(TAG, "createAudioRecorder: end", )
        }.start()
    }

    private fun getOutputFile(format: String): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        val curDate = Date(System.currentTimeMillis())
        val date = formatter.format(curDate).replace(" ", "")
        //设置路径前需要保证有夫目录
        val parentDir = getExternalFilesDir("records")?.apply { mkdirs() }
        return File(parentDir, "$date.$format").absolutePath
    }

    private fun createNotificationChannel() {
        val builder: Notification.Builder =
            Notification.Builder(this.applicationContext) //获取一个Notification构造器
        val nfIntent = Intent(this, Object::class.java) //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    android.R.mipmap.sym_def_app_icon
                )
            ) // 设置下拉列表中的图标(大图标)
            .setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(android.R.mipmap.sym_def_app_icon) // 设置状态栏内的小图标
            .setContentText("is running......") // 设置上下文内容
            .setWhen(System.currentTimeMillis()) // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                "notification_id",
                "notification_name",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification: Notification = builder.build() // 获取构建好的Notification
        notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
        startForeground(110, notification)
    }
}
