package com.contagiorni.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import java.io.File

/** Riproduce il brano di un widget. Un solo brano alla volta. */
class MusicService : Service() {

    private var player: MediaPlayer? = null
    private var currentId = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_TOGGLE) {
            val id = intent.getIntExtra(EXTRA_ID, -1)
            val data = WidgetStore.get(this, id)
            goForeground(data?.audioName?.ifBlank { "Musica" } ?: "Musica")
            if (currentId == id && player?.isPlaying == true) {
                stopAll()
            } else {
                startTrack(id, data)
            }
        } else {
            stopAll()
        }
        return START_NOT_STICKY
    }

    private fun startTrack(id: Int, data: WidgetData?) {
        releasePlayer()
        val path = data?.audioPath
        if (data == null || path == null || !File(path).exists()) {
            stopAll()
            return
        }
        try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(path)
            mp.isLooping = data.loop
            mp.setVolume(data.volume, data.volume)
            mp.setOnCompletionListener { stopAll() }
            mp.setOnErrorListener { _, _, _ ->
                stopAll()
                true
            }
            mp.prepare()
            mp.start()
            player = mp
            currentId = id
            WidgetStore.setPlaying(this, id)
            DayWidgetProvider.updateAll(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Non riesco a riprodurre questo file.", Toast.LENGTH_LONG).show()
            stopAll()
        }
    }

    private fun releasePlayer() {
        try {
            player?.stop()
        } catch (e: Exception) {
            // già fermo
        }
        player?.release()
        player = null
    }

    private fun stopAll() {
        releasePlayer()
        currentId = -1
        WidgetStore.setPlaying(this, -1)
        DayWidgetProvider.updateAll(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun goForeground(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Musica dei widget", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, MusicService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val action = Notification.Action.Builder(
            Icon.createWithResource(this, R.drawable.ic_pause), "Ferma", stop
        ).build()
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle("Conta Giorni")
            .setContentText(text)
            .setContentIntent(open)
            .addAction(action)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }

    companion object {
        const val ACTION_TOGGLE = "com.contagiorni.app.TOGGLE"
        const val ACTION_STOP = "com.contagiorni.app.STOP"
        const val EXTRA_ID = "widget_id"
        private const val CHANNEL = "musica"
        private const val NOTIFICATION_ID = 7

        fun togglePendingIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, MusicService::class.java)
                .setAction(ACTION_TOGGLE)
                .putExtra(EXTRA_ID, id)
            return PendingIntent.getForegroundService(
                context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun stop(context: Context) {
            try {
                context.startService(Intent(context, MusicService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                // l'app è in background: la musica si fermerà da sola
            }
        }
    }
}
