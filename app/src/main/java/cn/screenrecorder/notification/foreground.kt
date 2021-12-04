package cn.screenrecorder.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build

fun createForegroundNotification(context: Context):Notification {
    val builder: Notification.Builder =
        Notification.Builder(context.applicationContext) //获取一个Notification构造器
    val nfIntent = Intent(context, Object::class.java) //点击后跳转的界面，可以设置跳转数据
    builder.setContentIntent(PendingIntent.getActivity(context, 0, nfIntent, 0)) // 设置PendingIntent
        .setLargeIcon(
            BitmapFactory.decodeResource(
                context.resources,
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
        val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE)
                as NotificationManager
        val channel = NotificationChannel(
            "notification_id",
            "notification_name",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
    val notification: Notification = builder.build() // 获取构建好的Notification
    notification.defaults = Notification.DEFAULT_SOUND //设置为默认的声音
    return notification
}