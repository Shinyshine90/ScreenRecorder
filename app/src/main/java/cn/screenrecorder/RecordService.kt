package cn.screenrecorder

import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.media.MediaRecorder
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
import android.R
import android.app.*

import android.graphics.BitmapFactory
import android.hardware.display.VirtualDisplay

class RecordService : Service() {

    companion object {

        private const val TAG = "ScreenRecordService"

        private const val PARAM_CODE = "result_code"

        private const val PARAM_DATA = "data"

        fun create(ctx: Context, resultCode: Int, data: Intent) =
            Intent(ctx, RecordService::class.java).apply {
                putExtra(PARAM_CODE, resultCode)
                putExtra(PARAM_DATA, data)
            }
    }

    private val screenWidth = 1080

    private val screenHeight = 1920

    private val virtualDpi = 4

    private val recordFps = 60

    private var originIntent = Intent()

    private var mediaProjection: MediaProjection? = null

    private var mediaRecorder: MediaRecorder? = null

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
        virtualDisplay = setupVirtualDisplay()
        mediaRecorder?.start()
    }

    fun releaseRecord() {
        stopForeground(true)
        mediaRecorder?.stop()
        virtualDisplay?.release()
        mediaProjection?.stop()
        mediaRecorder = null
        mediaProjection = null
        virtualDisplay = null
    }

    private fun createProjection(): MediaProjection? {
        val resultCode = originIntent.getIntExtra(PARAM_CODE, Activity.RESULT_OK)
        val data = originIntent.getParcelableExtra<Intent>(PARAM_DATA)
        return data?.run {
            val protection = (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                .getMediaProjection(resultCode, this)
            protection
        }
    }

    private fun createMediaRecorder(): MediaRecorder {
        val formatter = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        val curDate = Date(System.currentTimeMillis())
        val curTime: String = formatter.format(curDate).replace(" ", "")
        val mediaRecorder = MediaRecorder()
        //audio
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        //video
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setOutputFile(File(getExternalFilesDir("records"), "$curTime.mp4").absolutePath)
        mediaRecorder.setVideoSize(screenWidth, screenHeight)
        mediaRecorder.setVideoEncodingBitRate(2000_000)
        mediaRecorder.setVideoFrameRate(recordFps)

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP)
        try {
            mediaRecorder.prepare()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mediaRecorder
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

    private fun createNotificationChannel() {
        val builder: Notification.Builder =
            Notification.Builder(this.applicationContext) //获取一个Notification构造器
        val nfIntent = Intent(this, Object::class.java) //点击后跳转的界面，可以设置跳转数据
        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0)) // 设置PendingIntent
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    this.resources,
                    R.mipmap.sym_def_app_icon
                )
            ) // 设置下拉列表中的图标(大图标)
            .setContentTitle("SMI InstantView") // 设置下拉列表里的标题
            .setSmallIcon(R.mipmap.sym_def_app_icon) // 设置状态栏内的小图标
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

    class ScreenRecordBinder(val service: RecordService) : Binder()
}
