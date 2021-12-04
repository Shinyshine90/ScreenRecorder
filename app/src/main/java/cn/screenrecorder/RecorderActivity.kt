package cn.screenrecorder

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View

private const val TAG = "RecorderActivity"

private const val REQUEST_CAPTURE_CODE = 1024

class RecorderActivity : AppCompatActivity() {

    private var recordService: RecordService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e(TAG, "onServiceConnected: ")
            val recordBinder = service as? RecordService.ScreenRecordBinder ?: return
            recordService = recordBinder.service.apply {
                startRecord()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e(TAG, "onServiceDisconnected: ")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recorder)
        findViewById<View>(R.id.btn_start).setOnClickListener {
            val intent = (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager)
                    .createScreenCaptureIntent()
            startActivityForResult(intent, REQUEST_CAPTURE_CODE)
        }
        findViewById<View>(R.id.btn_stop).setOnClickListener {
            recordService?.releaseRecord()
            unbindService(serviceConnection)
            stopService(Intent(this, RecordService::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAPTURE_CODE && resultCode == RESULT_OK && data != null) {
            val intent = RecordService.create(this, resultCode, data)
            startService(intent)
            bindService(intent, serviceConnection, Service.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        stopService(Intent(this, RecordService::class.java))

    }
}