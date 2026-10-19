package com.freedomfighter.readersmotivation

import com.freedomfighter.readersmotivation.data.Counts
import com.freedomfighter.readersmotivation.data.Event
import com.freedomfighter.readersmotivation.data.EventType
import com.freedomfighter.readersmotivation.data.Habit
import com.freedomfighter.readersmotivation.data.Kind
import com.freedomfighter.readersmotivation.data.Store
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CountsTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private fun at(date: String, hour: Int = 12) = LocalDate.parse(date).atStartOfDay(zone).plusHours(hour.toLong()).toInstant().toEpochMilli()
    private val start = LocalDate.parse("2026-10-01")
    private fun stop(vararg events: Event) = Habit("a", "sugar", Kind.STOP, start, unitsPerDay = 2.0, costPerUnit = 1.5, events = events.toList())

    @Test fun `a clean fortnight`() {
        val s = Counts.stats(stop(), LocalDate.parse("2026-10-14"), zone)
        assertEquals(14, s.daysTotal); assertEquals(14, s.daysHeld); assertEquals(14, s.run); assertEquals(14, s.bestRun)
        assertEquals(0, s.slips); assertEquals(28.0, s.unitsAvoided, 1e-9); assertEquals(42.0, s.moneySaved, 1e-9)
    }

    @Test fun `before the start nothing is counted`() {
        val s = Counts.stats(stop(), LocalDate.parse("2026-09-30"), zone)
        assertEquals(0, s.daysTotal); assertEquals(0, s.run); assertEquals(0, s.daysHeld)
    }

    @Test fun `a slip costs one day, never the total`() {
        // slip on the 5th: 1-4 held (4), 5 slip, 6-14 held (9)
        val s = Counts.stats(stop(Event(at("2026-10-05"), EventType.SLIP), Event(at("2026-10-05", 20), EventType.SLIP)), LocalDate.parse("2026-10-14"), zone)
        assertEquals(13, s.daysHeld); assertEquals(1, s.slips); assertEquals(9, s.run); assertEquals(9, s.bestRun)
    }

    @Test fun `the best run is remembered`() {
        val s = Counts.stats(stop(Event(at("2026-10-12"), EventType.SLIP)), LocalDate.parse("2026-10-14"), zone)
        assertEquals(2, s.run); assertEquals(11, s.bestRun); assertEquals(13, s.daysHeld)
    }

    @Test fun `a slip today makes the run zero`() {
        val s = Counts.stats(stop(Event(at("2026-10-14"), EventType.SLIP)), LocalDate.parse("2026-10-14"), zone)
        assertEquals(0, s.run); assertEquals(13, s.bestRun); assertEquals(13, s.daysHeld)
    }

    @Test fun `urges beaten are counted`() {
        val s = Counts.stats(stop(Event(at("2026-10-02"), EventType.RESIST), Event(at("2026-10-03"), EventType.RESIST)), LocalDate.parse("2026-10-03"), zone)
        assertEquals(2, s.resisted)
    }

    @Test fun `a do habit counts done days and the chain`() {
        val h = Habit("b", "walk", Kind.DO, start, events = listOf("2026-10-01", "2026-10-02", "2026-10-04", "2026-10-05", "2026-10-06").map { Event(at(it), EventType.DONE) })
        val s = Counts.stats(h, LocalDate.parse("2026-10-07"), zone)
        assertEquals(5, s.daysHeld); assertEquals(3, s.run); assertEquals(3, s.bestRun); assertFalse(s.doneToday)
        val later = Counts.stats(h, LocalDate.parse("2026-10-08"), zone)
        assertEquals(0, later.run) // two days missed: the chain is over, the days done stay
        assertEquals(5, later.daysHeld)
        assertTrue(Counts.stats(h, LocalDate.parse("2026-10-06"), zone).doneToday)
    }

    @Test fun `milestones`() {
        assertEquals(1, Counts.nextMilestone(0)); assertEquals(7, Counts.nextMilestone(3)); assertEquals(66, Counts.nextMilestone(30))
        assertEquals(365, Counts.nextMilestone(200)); assertEquals(730, Counts.nextMilestone(365)); assertEquals(1095, Counts.nextMilestone(800))
        assertEquals(7, Counts.reachedMilestone(7)); assertNull(Counts.reachedMilestone(8)); assertEquals(730, Counts.reachedMilestone(730))
    }

    @Test fun `the grid ends on this week's Sunday`() {
        // 2026-10-14 is a Wednesday; start on the 1st (Thursday); slip on the 5th (Monday)
        val g = Counts.grid(stop(Event(at("2026-10-05"), EventType.SLIP)), LocalDate.parse("2026-10-14"), 3, zone)
        assertEquals(listOf("...hhhh", "shhhhhh", "hhh...."), g)
        val d = Habit("b", "walk", Kind.DO, start, events = listOf(Event(at("2026-10-13"), EventType.DONE)))
        assertEquals("sht....", Counts.grid(d, LocalDate.parse("2026-10-14"), 1, zone)[0])
    }

    @Test fun `export and import round trip`() {
        val h = stop(Event(at("2026-10-05"), EventType.SLIP, "birthday cake"), Event(at("2026-10-06"), EventType.RESIST))
        val back = Store.parse(Store.serialize(listOf(h)))
        assertEquals(listOf(h), back)
    }
}
