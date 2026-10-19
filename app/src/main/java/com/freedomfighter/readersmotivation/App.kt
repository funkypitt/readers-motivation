package com.freedomfighter.readersmotivation

import android.app.Application
import com.freedomfighter.readersmotivation.data.Exports
import com.freedomfighter.readersmotivation.data.Habit
import com.freedomfighter.readersmotivation.data.Prefs
import com.freedomfighter.readersmotivation.data.Store
import com.freedomfighter.readersmotivation.widget.MotivationWidget

class App : Application() {
    val prefs: Prefs by lazy { Prefs(this) }
    val store: Store by lazy { Store(this).also { s -> s.onChange = { MotivationWidget.refresh(this) } } }

    override fun onCreate() {
        super.onCreate()
        Exports.cleanUp(this)
        Reminder.schedule(this)
    }

    /** The habit the widget and the reminder speak of: the chosen one, else the first. */
    fun featured(): Habit? {
        val list = store.habits.value
        val id = prefs.settings.value.widgetHabit
        return list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }
}
