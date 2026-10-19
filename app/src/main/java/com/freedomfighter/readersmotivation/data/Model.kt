package com.freedomfighter.readersmotivation.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Something to stop doing (days without, urges beaten, slips) or something to do every day (days done). */
enum class Kind { STOP, DO }

/** What was noted: an urge beaten, a slip, or the day done (DO habits). */
enum class EventType { RESIST, SLIP, DONE }

data class Event(val t: Long, val type: EventType, val note: String = "") {
    fun date(zone: ZoneId = ZoneId.systemDefault()): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()
}

data class Habit(
    val id: String,
    val name: String,
    val kind: Kind = Kind.STOP,
    val start: LocalDate,
    /** The personal reason, in the person's words. */
    val why: String = "",
    /** The if-then plan: "when the urge comes, I…". */
    val plan: String = "",
    /** Units avoided per day held (cigarettes, drinks, pieces…); 0 = not counted. */
    val unitsPerDay: Double = 0.0,
    val unitName: String = "",
    /** Cost of one unit; 0 = not counted. */
    val costPerUnit: Double = 0.0,
    val currency: String = "",
    val events: List<Event> = emptyList()
)

/** The numbers of one habit on a given day; every count that was earned stays earned. */
data class Stats(
    /** Days from the start to today, both included; 0 before the start. */
    val daysTotal: Int,
    /** STOP: days without a slip. DO: days done. Never goes down. */
    val daysHeld: Int,
    /** STOP: days since the last slip (today included, 0 when the slip is today). DO: consecutive days done up to today or yesterday. */
    val run: Int,
    val bestRun: Int,
    val resisted: Int,
    val slips: Int,
    /** DO habits: the day is already marked done. */
    val doneToday: Boolean,
    val unitsAvoided: Double,
    val moneySaved: Double
)

/** The pure arithmetic, tested on the JVM. */
object Counts {
    /** The steps: days of one run. */
    val MILESTONES = listOf(1, 3, 7, 14, 21, 30, 66, 100, 180, 365)

    fun stats(h: Habit, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Stats {
        val daysTotal = if (today < h.start) 0 else (ChronoUnit.DAYS.between(h.start, today) + 1).toInt()
        val resisted = h.events.count { it.type == EventType.RESIST }
        return if (h.kind == Kind.STOP) {
            val slipDays = h.events.filter { it.type == EventType.SLIP }.map { it.date(zone) }.filter { it >= h.start && it <= today }.toSortedSet()
            val held = (daysTotal - slipDays.size).coerceAtLeast(0)
            // the runs: the clean stretches between slip days, the last one open until today
            var best = 0; var run = 0
            if (daysTotal > 0) {
                var from = h.start
                for (d in slipDays) {
                    best = maxOf(best, ChronoUnit.DAYS.between(from, d).toInt())
                    from = d.plusDays(1)
                }
                run = if (from > today) 0 else (ChronoUnit.DAYS.between(from, today) + 1).toInt()
                best = maxOf(best, run)
            }
            val units = held * h.unitsPerDay
            Stats(daysTotal, held, run, best, resisted, slipDays.size, false, units, units * h.costPerUnit)
        } else {
            val done = h.events.filter { it.type == EventType.DONE }.map { it.date(zone) }.filter { it >= h.start && it <= today }.toSortedSet()
            var best = 0; var cur = 0; var prev: LocalDate? = null
            for (d in done) {
                cur = if (prev != null && prev.plusDays(1) == d) cur + 1 else 1
                best = maxOf(best, cur)
                prev = d
            }
            // the run still stands when today is not marked yet, as long as yesterday is
            val run = if (prev == today || prev == today.minusDays(1)) cur else 0
            val units = done.size * h.unitsPerDay
            Stats(daysTotal, done.size, run, best, resisted, 0, today in done, units, units * h.costPerUnit)
        }
    }

    /** The next step after this run, in days; past the list, the next anniversary. */
    fun nextMilestone(run: Int): Int = MILESTONES.firstOrNull { it > run } ?: ((run / 365 + 1) * 365)

    /** The step just reached, when the run lands on one today. */
    fun reachedMilestone(run: Int): Int? = if (run in MILESTONES || (run > 365 && run % 365 == 0)) run else null

    /**
     * The state of each day of the last [weeks] weeks, Monday first, oldest first:
     * 'h' held/done, 's' slip/missed, 't' today not yet done (DO), '.' before the start or after today.
     */
    fun grid(h: Habit, today: LocalDate, weeks: Int = 5, zone: ZoneId = ZoneId.systemDefault()): List<String> {
        val end = today.plusDays((7 - today.dayOfWeek.value).toLong()) // the Sunday of this week
        val first = end.minusDays((weeks * 7 - 1).toLong())
        val slips = h.events.filter { it.type == EventType.SLIP }.map { it.date(zone) }.toSet()
        val done = h.events.filter { it.type == EventType.DONE }.map { it.date(zone) }.toSet()
        return (0 until weeks).map { w ->
            (0 until 7).map { i ->
                val d = first.plusDays((w * 7 + i).toLong())
                when {
                    d < h.start || d > today -> '.'
                    h.kind == Kind.STOP -> if (d in slips) 's' else 'h'
                    d in done -> 'h'
                    d == today -> 't'
                    else -> 's'
                }
            }.joinToString("")
        }
    }
}
