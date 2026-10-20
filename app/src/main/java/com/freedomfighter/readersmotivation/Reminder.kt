package com.freedomfighter.readersmotivation

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.freedomfighter.readersmotivation.data.Counts
import com.freedomfighter.readersmotivation.data.Kind
import com.freedomfighter.readersmotivation.widget.MotivationWidget
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** The one daily line, at the hour chosen; off unless turned on. */
object Reminder {
    private const val ACTION = "com.freedomfighter.readersmotivation.REMIND"
    private const val CHANNEL = "reminder"
    const val ACTION_NEW_DAY = "com.freedomfighter.readersmotivation.NEW_DAY"

    private fun pending(context: Context, action: String = ACTION, code: Int = 7): PendingIntent =
        PendingIntent.getBroadcast(context, code, Intent(context, ReminderReceiver::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    /** The widget shows the day: it is redrawn just after midnight, whatever the reminder setting. */
    private fun scheduleNewDay(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = ZonedDateTime.now().plusDays(1).with(LocalTime.of(0, 1)).withSecond(0).withNano(0)
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC, at.toInstant().toEpochMilli(), pending(context, ACTION_NEW_DAY, 9)) }
    }

    /** (Re)arms the alarm for the next occurrence of the chosen time, or cancels it. */
    fun schedule(context: Context) {
        scheduleNewDay(context)
        val app = context.applicationContext as App
        val minutes = app.prefs.settings.value.reminderMinutes
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (minutes < 0) { am.cancel(pending(context)); return }
        var at = ZonedDateTime.now().with(LocalTime.of(minutes / 60, minutes % 60)).withSecond(0).withNano(0)
        if (!at.isAfter(ZonedDateTime.now())) at = at.plusDays(1)
        runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toInstant().toEpochMilli(), pending(context)) }
    }

    fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun fire(context: Context) {
        val app = context.applicationContext as App
        val h = app.featured() ?: return
        val s = Counts.stats(h, LocalDate.now())
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, context.getString(R.string.notif_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 8, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = context.getString(if (h.kind == Kind.STOP) R.string.notif_question_stop else R.string.notif_question_do)
        val n = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(Words.summary(context, h, s))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        if (allowed(context)) runCatching { nm.notify(1, n) }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.freedomfighter.readersmotivation.REMIND") Reminder.fire(context)
        if (intent.action == Reminder.ACTION_NEW_DAY) MotivationWidget.refresh(context)
        // a boot, a time change, or the alarm itself: the next one is armed again
        Reminder.schedule(context)
    }
}
