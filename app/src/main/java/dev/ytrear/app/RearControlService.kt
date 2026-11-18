package dev.ytrear.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

/**
 * Keeps the allowlisted proxy MediaSession alive while the selected player is active.
 */
class RearControlService : Service() {

    private var observing = false
    private var lastTitleLabel: String? = null

    private val mediaListener = MediaHub.Listener { state ->
        updateForegroundTitleIfNeeded()
        RearController.render(this, state)
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIF_ID, foregroundNotification())
        beginObserving()
        // Re-snooze the selected player whenever the proxy is turned back on.
        MediaNotificationListener.targetChanged(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, foregroundNotification())
        beginObserving()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun beginObserving() {
        val attached = MediaHub.attach(this)
        if (!observing) {
            MediaHub.addListener(mediaListener)
            observing = true
        }
        Probe.log("CONTROL SERVICE: active; session access=$attached")
    }

    override fun onDestroy() {
        if (observing) {
            MediaHub.removeListener(mediaListener)
            observing = false
        }
        isRunning = false
        if (stopping) {
            stopping = false
            NativeMediaProxy.clear(this)
            MediaHub.detach()
            MediaNotificationListener.restoreCurrentTarget(this)
        } else if (!MediaNotificationListener.isConnected) {
            NativeMediaProxy.clear(this)
            MediaHub.detach()
        }
        super.onDestroy()
    }

    private fun foregroundNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                getString(R.string.notification_channel_service),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val label = PlayerSelection.targetLabel(this)
            ?: getString(R.string.notification_selected_player)
        lastTitleLabel = label
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(getString(R.string.notification_service_title, label))
            .setContentText(getString(R.string.notification_service_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun updateForegroundTitleIfNeeded() {
        val current = PlayerSelection.targetLabel(this)
            ?: getString(R.string.notification_selected_player)
        if (current == lastTitleLabel) return
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, foregroundNotification())
    }

    companion object {
        private const val CHANNEL = "rear_control_service"
        private const val NOTIF_ID = 76

        @Volatile
        private var stopping = false

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            context.startForegroundService(Intent(context, RearControlService::class.java))
        }

        /** Turns the proxy off and returns the selected player's own media notification. */
        fun stop(context: Context) {
            val app = context.applicationContext
            stopping = true
            if (!app.stopService(Intent(app, RearControlService::class.java))) {
                stopping = false
                NativeMediaProxy.clear(app)
                MediaHub.detach()
                MediaNotificationListener.restoreCurrentTarget(app)
            }
        }
    }
}
