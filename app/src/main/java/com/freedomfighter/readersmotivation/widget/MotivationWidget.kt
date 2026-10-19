package com.freedomfighter.readersmotivation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.freedomfighter.readersmotivation.App
import com.freedomfighter.readersmotivation.MainActivity
import com.freedomfighter.readersmotivation.R
import com.freedomfighter.readersmotivation.Words
import com.freedomfighter.readersmotivation.data.Counts
import com.freedomfighter.readersmotivation.data.Event
import com.freedomfighter.readersmotivation.data.EventType
import com.freedomfighter.readersmotivation.data.Kind
import java.time.LocalDate

/**
 * The home-screen widget: "day 12" over "without sugar · 9 urges beaten", and ✓ at the right
 * that counts one more urge beaten (or marks the day done) without opening the app.
 */
object MotivationWidget {
    const val ACTION_TICK = "com.freedomfighter.readersmotivation.widget.TICK"

    private fun colors(context: Context): Triple<Int, Int, Int> {
        val theme = context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("theme", "DARK")
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = when (theme) { "LIGHT" -> false; "SYSTEM" -> systemDark; else -> true }
        return if (dark) Triple(Color.BLACK, Color.WHITE, Color.argb(140, 255, 255, 255)) else Triple(Color.WHITE, Color.BLACK, Color.argb(140, 0, 0, 0))
    }

    fun render(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext as App
        val h = app.featured()
        val today = LocalDate.now()
        val (bg, fg, dim) = colors(context)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ids.forEach { id ->
            val v = RemoteViews(context.packageName, R.layout.widget_line)
            v.setInt(R.id.widget_root, "setBackgroundColor", bg)
            v.setTextColor(R.id.widget_title, fg); v.setTextColor(R.id.widget_sub, dim); v.setTextColor(R.id.widget_tick, fg)
            if (h == null) {
                v.setTextViewText(R.id.widget_title, context.getString(R.string.app_title))
                v.setTextViewText(R.id.widget_sub, context.getString(R.string.new_habit))
                v.setViewVisibility(R.id.widget_tick, View.GONE)
            } else {
                val s = Counts.stats(h, today)
                v.setTextViewText(R.id.widget_title, if (s.daysTotal == 0) context.getString(R.string.starts_on, Words.date(h.start)) else Words.dayLine(context, s, h))
                val what = if (h.kind == Kind.STOP) context.getString(R.string.pair, h.name, Words.resistedLine(context, s))
                    else if (s.doneToday) context.getString(R.string.pair, h.name, context.getString(R.string.done_already)) else h.name
                v.setTextViewText(R.id.widget_sub, what)
                // DO habits: the tick is gone once the day is marked
                v.setViewVisibility(R.id.widget_tick, if (h.kind == Kind.DO && s.doneToday) View.GONE else View.VISIBLE)
                val tick = Intent(context, MotivationWidgetProvider::class.java).setAction(ACTION_TICK).putExtra("id", h.id)
                v.setOnClickPendingIntent(R.id.widget_tick, PendingIntent.getBroadcast(context, 2, tick, flags))
            }
            val open = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            v.setOnClickPendingIntent(R.id.widget_text, PendingIntent.getActivity(context, 1, open, flags))
            mgr.updateAppWidget(id, v)
        }
    }

    fun refresh(context: Context) {
        val ctx = context.applicationContext
        val mgr = AppWidgetManager.getInstance(ctx) ?: return
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, MotivationWidgetProvider::class.java))
        if (ids.isNotEmpty()) render(ctx, mgr, ids)
    }
}

class MotivationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) = MotivationWidget.render(context, mgr, ids)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != MotivationWidget.ACTION_TICK) return
        val app = context.applicationContext as App
        val h = app.store.get(intent.getStringExtra("id") ?: return) ?: return
        val now = System.currentTimeMillis()
        if (h.kind == Kind.STOP) app.store.add(h.id, Event(now, EventType.RESIST))
        else if (!Counts.stats(h, LocalDate.now()).doneToday) app.store.add(h.id, Event(now, EventType.DONE))
        // the store's onChange already redraws the widget
    }
}
