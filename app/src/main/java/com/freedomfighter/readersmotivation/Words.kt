package com.freedomfighter.readersmotivation

import android.content.Context
import com.freedomfighter.readersmotivation.data.Counts
import com.freedomfighter.readersmotivation.data.Habit
import com.freedomfighter.readersmotivation.data.Kind
import com.freedomfighter.readersmotivation.data.Stats
import com.freedomfighter.readersmotivation.data.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Everything the screens, the widget and the reminder say about a habit. */
object Words {
    fun days(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.days, n, n)
    fun urges(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.urges, n, n)
    fun daysHeld(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.days_held, n, n)
    fun daysDone(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.days_done, n, n)
    fun heldStay(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.held_stay, n, n)
    fun urgesStay(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.urges_stay, n, n)
    fun doneStay(c: Context, n: Int): String = c.resources.getQuantityString(R.plurals.done_stay, n, n)

    fun date(d: LocalDate): String = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()))

    /** "without sugar since 8 October 2026" / "walk, every day since…" / "starts on…". */
    fun since(c: Context, h: Habit, today: LocalDate): String = when {
        today < h.start -> c.getString(R.string.starts_on, date(h.start))
        h.kind == Kind.STOP -> c.getString(R.string.without_since, h.name, date(h.start))
        else -> c.getString(R.string.doing_since, h.name, date(h.start))
    }

    /**
     * The big line: "day 12". On the day of a slip, what stays earned instead: "19 days held";
     * the broken run is never the headline. A habit to do with nothing done yet: "day 1", the
     * day it becomes once it is done.
     */
    fun dayLine(c: Context, s: Stats, h: Habit? = null): String = when {
        s.run > 0 || s.daysTotal == 0 -> c.getString(R.string.day_n, s.run)
        h?.kind == Kind.DO -> if (s.daysHeld == 0) c.getString(R.string.day_n, 1) else daysDone(c, s.daysHeld)
        else -> daysHeld(c, s.daysHeld)
    }

    /** Under the big line: "13 days held out of 14 · best run 11 days", only once a day was lost; on a slip day "out of 20 · best run 19 days". */
    fun heldLine(c: Context, h: Habit, s: Stats): String? {
        if (s.daysHeld == s.daysTotal && s.bestRun == s.run) return null
        if (h.kind == Kind.DO && s.daysHeld == 0) return null
        val best = c.getString(R.string.best_run, days(c, s.bestRun))
        val held = if (s.run == 0) c.getString(R.string.of_total, s.daysTotal)
            else c.getString(R.string.held_of, if (h.kind == Kind.STOP) daysHeld(c, s.daysHeld) else daysDone(c, s.daysHeld), s.daysTotal)
        return if (s.bestRun > s.run) c.getString(R.string.pair, held, best) else held
    }

    /** On a slip day, the one line that looks forward. */
    fun restartLine(c: Context, s: Stats, h: Habit): String? = when {
        s.run > 0 || s.daysTotal == 0 -> null
        h.kind == Kind.STOP -> c.getString(R.string.restart_now)
        s.daysHeld > 0 -> c.getString(R.string.restart_now_do)
        else -> null
    }

    fun resistedLine(c: Context, s: Stats): String = if (s.resisted == 0) c.getString(R.string.no_resisted) else urges(c, s.resisted)

    /** "492 cigarettes avoided · 246 CHF not spent", or null when nothing is counted. */
    fun unitsLine(c: Context, h: Habit, s: Stats): String? {
        if (h.unitsPerDay <= 0.0) return null
        val units = c.getString(R.string.units_avoided, number(s.unitsAvoided), h.unitName)
        if (h.costPerUnit <= 0.0) return units
        return c.getString(R.string.pair, units, c.getString(R.string.money_saved, number(s.moneySaved), h.currency))
    }

    fun number(x: Double): String = if (x == Math.floor(x) && x < 1e9) "%d".format(x.toLong()) else String.format(Locale.getDefault(), "%.1f", x)

    fun milestone(c: Context, m: Int): String = when (m) {
        1 -> c.getString(R.string.m_1); 3 -> c.getString(R.string.m_3); 7 -> c.getString(R.string.m_7); 14 -> c.getString(R.string.m_14)
        21 -> c.getString(R.string.m_21); 30 -> c.getString(R.string.m_30); 66 -> c.getString(R.string.m_66); 100 -> c.getString(R.string.m_100)
        180 -> c.getString(R.string.m_180); 365 -> c.getString(R.string.m_365)
        else -> c.resources.getQuantityString(R.plurals.years, m / 365, m / 365)
    }

    /**
     * The step line. Reached today: "one week, reached today". Otherwise the smaller of the two
     * parts: "day 2 of one week" in the first half, "5 days to go before 66 days" in the second.
     */
    fun stepLine(c: Context, s: Stats): String? {
        if (s.daysTotal == 0 || s.run == 0) return null
        Counts.reachedMilestone(s.run)?.let { return c.getString(R.string.step_reached, milestone(c, it)) }
        val next = Counts.nextMilestone(s.run)
        val prev = Counts.MILESTONES.lastOrNull { it < next } ?: 0
        val half = prev + (next - prev) / 2.0
        return if (s.run < half) c.getString(R.string.step_of, s.run, milestone(c, next)) else c.getString(R.string.step_to, days(c, next - s.run), milestone(c, next))
    }

    /** The 66-day note, when that step is the next one or was reached today. */
    fun stepNote(c: Context, s: Stats): String? =
        if (Counts.reachedMilestone(s.run) == 66 || (Counts.nextMilestone(s.run) == 66 && s.run >= 30)) c.getString(R.string.m_66_note) else null

    /** One line for the list, the widget and the reminder: "day 12 · 9 urges beaten". */
    fun summary(c: Context, h: Habit, s: Stats): String {
        val day = if (s.daysTotal == 0) c.getString(R.string.starts_on, date(h.start)) else dayLine(c, s, h)
        return if (h.kind == Kind.STOP) c.getString(R.string.pair, day, resistedLine(c, s))
        else if (s.doneToday) c.getString(R.string.pair, day, c.getString(R.string.done_already)) else day
    }

    /** The text to share: the since line, the day, the counts. */
    fun shareText(c: Context, h: Habit, s: Stats, today: LocalDate): String = listOfNotNull(
        since(c, h, today),
        if (s.daysTotal > 0) dayLine(c, s, h) else null,
        heldLine(c, h, s),
        if (h.kind == Kind.STOP) resistedLine(c, s) else null,
        unitsLine(c, h, s)
    ).joinToString("\n")

    /** "the record" rows: the grid of weeks as text, Monday first. */
    fun gridRows(h: Habit, today: LocalDate): List<String> = Counts.grid(h, today).map { w ->
        w.map { ch -> when (ch) { 'h' -> '■'; 's' -> '□'; 't' -> '◦'; else -> '·' } }.joinToString(" ")
    }

    /** The name of a theme, as the settings row and the menu show it. */
    fun themeName(c: Context, m: ThemeMode): String = c.getString(when (m) {
        ThemeMode.DARK -> R.string.theme_dark; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LAVANDE -> R.string.theme_lavande; ThemeMode.SAUGE -> R.string.theme_sauge; ThemeMode.NORDIQUE -> R.string.theme_nordique
        ThemeMode.TERRACOTTA -> R.string.theme_terracotta; ThemeMode.COQUETTE -> R.string.theme_coquette; ThemeMode.MILITANTE -> R.string.theme_militante
        ThemeMode.TORRIDE -> R.string.theme_torride; ThemeMode.NATURE -> R.string.theme_nature
    })

    fun minutesWord(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

    fun parseTime(text: String): Int? {
        var t = text.trim().replace('.', ':').replace('h', ':')
        // "730" and "0730" as well as "7:30"
        if (t.length in 3..4 && t.all { it.isDigit() }) t = t.dropLast(2) + ":" + t.takeLast(2)
        val parts = t.split(':')
        val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.trim()?.ifEmpty { "0" }?.toIntOrNull() ?: 0
        return if (h in 0..23 && m in 0..59) h * 60 + m else null
    }
}
