package cn.screenrecorder

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.File
import android.os.Build
import android.app.*
import cn.screenrecorder.entity.MediaOutputs
import cn.screenrecorder.entity.generate
import cn.screenrecorder.notification.createForegroundNotification
import cn.screenrecorder.record.PlaybackRecorder
import cn.screenrecorder.record.ScreenMicRecorder

class RecordService : Service() {

    companion object {

        private const val TAG = "ScreenRecordService"

        private const val PARAM_CODE = "result_code"

        private const val PARAM_PROJECTION_TOKEN = "media_projection_token"

        private const val FOREGROUND_ID = 0x100

        fun create(ctx: Context, resultCode: Int, token: Intent) =
            Intent(ctx, RecordService::class.java).apply {
                putExtra(PARAM_CODE, resultCode)
                putExtra(PARAM_PROJECTION_TOKEN, token)
            }
    }

    private val mediaOutputs = MediaOutputs()

    private lateinit var mediaProjection: MediaProjection

    private lateinit var screenMicRecorder: ScreenMicRecorder

    private lateinit var playbackRecorder: PlaybackRecorder

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate: ")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "onStartCommand: $startId")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.e(TAG, "onBind: ")
        //这里启动前台服务是因为Service中获取MediaProjection需要前台服务，需要在校验前设置为前台服务
        startForeground(FOREGROUND_ID, createForegroundNotification(this))
        //校验参数，获取MediaProjection
        checkValid(intent) { projection ->
            //校验成功后做初始化
            prepare(projection)
            //校验成功返回Binder对象
            return ScreenRecordBinder(this)
        }
        stopForeground(true)
        stopSelf()
        return null
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.e(TAG, "onUnbind: ")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.e(TAG, "onDestroy: ")
        super.onDestroy()
    }

    private inline fun checkValid(intent: Intent, success: (MediaProjection) -> Unit) {
        val resultCode = intent.getIntExtra(PARAM_CODE, Activity.RESULT_CANCELED)
        val projectionToken = intent.getParcelableExtra<Intent>(PARAM_PROJECTION_TOKEN)
        if (resultCode != Activity.RESULT_OK || projectionToken == null) {
            return
        }
        (getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
            .getMediaProjection(resultCode, projectionToken)?.apply {
                success(this)
            }
    }

    private fun prepare(projection: MediaProjection) {
        mediaProjection = projection
        //生成文件列表
        mediaOutputs.generate(this)
        Log.e(TAG, "startRecord: $mediaOutputs")
        //创建ScreenMicRecorder
        screenMicRecorder = ScreenMicRecorder(
            { File(mediaOutputs.mp4TempPath) },
            { mediaProjection }
        )
        screenMicRecorder.prepare()
        //创建PlaybackRecord
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackRecorder = PlaybackRecorder(
                { File(mediaOutputs.accTempPath) },
                { mediaProjection }
            )
            playbackRecorder.prepare()
        }
    }

    fun startRecord() {
        screenMicRecorder.start()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackRecorder.start()
        }
    }

    fun releaseRecord() {
        stopForeground(true)
        mediaProjection.stop()
        screenMicRecorder.stop()
        screenMicRecorder.release()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            playbackRecorder.apply {
                stop()
                release()
            }
        }
    }
    class ScreenRecordBinder(val service: RecordService) : Binder()

}
