package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.lab_week_08.MainActivity

class SecondNotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = createForegroundNotification()
        val handlerThread = HandlerThread("SecondNotificationCountdownThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        val channelId = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        serviceHandler.post {
            countDownFromFiveToZero(notificationBuilder)
            notifyCompletion(channelId)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return returnValue
    }

    private fun createForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        return getNotificationBuilder(pendingIntent, channelId).also { builder ->
            startForeground(NOTIFICATION_ID, builder.build())
        }
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            flag
        )
    }

    private fun createNotificationChannel(): String {
        val channelId = "002"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "002 Channel"
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, channelPriority)

            val manager = requireNotNull(
                ContextCompat.getSystemService(this, NotificationManager::class.java)
            )
            manager.createNotificationChannel(channel)
        }

        return channelId
    }

    private fun getNotificationBuilder(
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Third worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Third worker process is done, check it out!")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
    }

    private fun countDownFromFiveToZero(builder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        for (seconds in 5 downTo 0) {
            Thread.sleep(1000L)
            builder.setContentText("$seconds seconds until last warning")
                .setSilent(true)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(channelId: String) {
        Handler(Looper.getMainLooper()).post {
            mutableId.value = channelId
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA8
        const val EXTRA_ID = "Id"

        private val mutableId = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableId
    }
}