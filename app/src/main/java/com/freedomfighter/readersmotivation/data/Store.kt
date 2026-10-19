package com.freedomfighter.readersmotivation.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** The habits, kept in one JSON file in the app's private storage; nothing leaves the phone. */
class Store(context: Context) {
    private val file = File(context.filesDir, "habits.json")
    private val _habits = MutableStateFlow(load())
    val habits: StateFlow<List<Habit>> = _habits
    /** Called after each write, for the widget and the reminder. */
    var onChange: () -> Unit = {}

    @Synchronized private fun load(): List<Habit> = runCatching { if (file.exists()) parse(file.readText()) else emptyList() }.getOrDefault(emptyList())

    @Synchronized private fun write(list: List<Habit>) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(serialize(list))
        tmp.renameTo(file)
        _habits.value = list
        onChange()
    }

    fun get(id: String): Habit? = _habits.value.firstOrNull { it.id == id }

    fun save(h: Habit) {
        val list = _habits.value
        write(if (list.any { it.id == h.id }) list.map { if (it.id == h.id) h else it } else list + h)
    }

    fun delete(id: String) = write(_habits.value.filter { it.id != id })

    fun add(id: String, e: Event) { get(id)?.let { save(it.copy(events = it.events + e)) } }

    fun removeEvent(id: String, e: Event) { get(id)?.let { h -> save(h.copy(events = h.events.filter { it !== e && it != e })) } }

    /** Replaces everything with an export; false when the text is not one. */
    fun import(text: String): Boolean {
        val list = runCatching { parse(text) }.getOrNull() ?: return false
        write(list)
        return true
    }

    fun export(): String = serialize(_habits.value)

    companion object {
        fun newId(): String = UUID.randomUUID().toString()

        fun serialize(list: List<Habit>): String {
            val arr = JSONArray()
            list.forEach { h ->
                arr.put(JSONObject().apply {
                    put("id", h.id); put("name", h.name); put("kind", h.kind.name); put("start", h.start.toString())
                    put("why", h.why); put("plan", h.plan)
                    put("unitsPerDay", h.unitsPerDay); put("unitName", h.unitName); put("costPerUnit", h.costPerUnit); put("currency", h.currency)
                    put("events", JSONArray().apply { h.events.forEach { e -> put(JSONObject().apply { put("t", e.t); put("type", e.type.name); if (e.note.isNotEmpty()) put("note", e.note) }) } })
                })
            }
            return JSONObject().put("app", "readers-motivation").put("version", 1).put("habits", arr).toString(2)
        }

        fun parse(text: String): List<Habit> {
            val root = JSONObject(text)
            val arr = root.getJSONArray("habits")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val ev = o.optJSONArray("events") ?: JSONArray()
                Habit(
                    id = o.getString("id"), name = o.getString("name"),
                    kind = runCatching { Kind.valueOf(o.optString("kind", "STOP")) }.getOrDefault(Kind.STOP),
                    start = LocalDate.parse(o.getString("start")),
                    why = o.optString("why", ""), plan = o.optString("plan", ""),
                    unitsPerDay = o.optDouble("unitsPerDay", 0.0), unitName = o.optString("unitName", ""),
                    costPerUnit = o.optDouble("costPerUnit", 0.0), currency = o.optString("currency", ""),
                    events = (0 until ev.length()).mapNotNull { j ->
                        val e = ev.getJSONObject(j)
                        runCatching { Event(e.getLong("t"), EventType.valueOf(e.getString("type")), e.optString("note", "")) }.getOrNull()
                    }
                )
            }
        }
    }
}
