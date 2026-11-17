package com.contagiorni.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews

class DayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { update(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle
    ) {
        update(context, manager, id)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        ids.forEach { id ->
            if (WidgetStore.playingId(context) == id) MusicService.stop(context)
            WidgetStore.delete(context, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> updateAll(context)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DayWidgetProvider::class.java))
            ids.forEach { update(context, manager, it) }
        }

        fun update(context: Context, manager: AppWidgetManager, id: Int) {
            val data = WidgetStore.get(context, id) ?: WidgetData.default()
            val options = manager.getAppWidgetOptions(id)
            val w = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val h = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val wDp = if (w > 0) w.toFloat() else 160f
            val hDp = if (h > 0) h.toFloat() else 160f

            val views = RemoteViews(context.packageName, R.layout.widget_day)
            views.setImageViewBitmap(R.id.bg, WidgetRenderer.render(context, data, wDp, hDp))

            val info = DayMath.describe(DayMath.daysFrom(data.date))
            views.setCharSequence(
                R.id.bg, "setContentDescription",
                "${data.title}: ${info.first} ${info.second}. ${data.phrase}"
            )

            val editIntent = Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            val editPi = PendingIntent.getActivity(
                context, id, editIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, editPi)

            if (data.audioPath != null) {
                val playing = WidgetStore.playingId(context) == id
                views.setViewVisibility(R.id.play, View.VISIBLE)
                views.setImageViewResource(
                    R.id.play, if (playing) R.drawable.ic_pause else R.drawable.ic_play
                )
                views.setCharSequence(
                    R.id.play, "setContentDescription",
                    if (playing) "Metti in pausa la musica" else "Riproduci la musica"
                )
                views.setOnClickPendingIntent(R.id.play, MusicService.togglePendingIntent(context, id))
            } else {
                views.setViewVisibility(R.id.play, View.GONE)
            }

            manager.updateAppWidget(id, views)
        }
    }
}
