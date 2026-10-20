package com.freedomfighter.readersmotivation.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The inks, the phone's choice between them, then the Plume palettes (see [Palettes]). */
enum class ThemeMode { DARK, LIGHT, SYSTEM, LAVANDE, SAUGE, NORDIQUE, TERRACOTTA, COQUETTE, MILITANTE, TORRIDE, NATURE }
enum class FontChoice { SERIF, SANS, MONO }
enum class TextSize { SMALL, MEDIUM, LARGE }
enum class Align { LEFT, CENTER }

/** The look and the one reminder. */
data class Settings(
    val theme: ThemeMode = ThemeMode.DARK,
    val font: FontChoice = FontChoice.SANS,
    val textSize: TextSize = TextSize.MEDIUM,
    val align: Align = Align.LEFT,
    val haptics: Boolean = true,
    /** The daily reminder, in minutes from midnight; -1 = none. */
    val reminderMinutes: Int = -1,
    /** The habit shown by the widget and the reminder; empty = the first one. */
    val widgetHabit: String = ""
)

class Prefs(context: Context) {
    private val sp: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> _settings.value = read() }
    init { sp.registerOnSharedPreferenceChangeListener(listener) }

    private fun read() = Settings(
        theme = enumOr(sp.getString("theme", null), ThemeMode.DARK),
        font = enumOr(sp.getString("font", null), FontChoice.SANS),
        textSize = enumOr(sp.getString("text_size", null), TextSize.MEDIUM),
        align = enumOr(sp.getString("align", null), Align.LEFT),
        haptics = sp.getBoolean("haptics", true),
        reminderMinutes = sp.getInt("reminder_minutes", -1),
        widgetHabit = sp.getString("widget_habit", "") ?: ""
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

    /** One tap: the next theme, round and round. */
    fun nextTheme() = sp.edit().putString("theme", Palettes.next(settings.value.theme).name).apply()
    fun setFont(f: FontChoice) = sp.edit().putString("font", f.name).apply()
    fun setTextSize(s: TextSize) = sp.edit().putString("text_size", s.name).apply()
    fun setHaptics(on: Boolean) = sp.edit().putBoolean("haptics", on).apply()
    fun setReminderMinutes(m: Int) = sp.edit().putInt("reminder_minutes", m).apply()
    fun setWidgetHabit(id: String) = sp.edit().putString("widget_habit", id).apply()
}
