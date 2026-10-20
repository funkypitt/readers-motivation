package com.freedomfighter.readersmotivation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.freedomfighter.readersmotivation.App
import com.freedomfighter.readersmotivation.MainActivity
import com.freedomfighter.readersmotivation.R
import com.freedomfighter.readersmotivation.Reminder
import com.freedomfighter.readersmotivation.Words
import com.freedomfighter.readersmotivation.data.Counts
import com.freedomfighter.readersmotivation.data.Event
import com.freedomfighter.readersmotivation.data.EventType
import com.freedomfighter.readersmotivation.data.Exports
import com.freedomfighter.readersmotivation.data.FontChoice
import com.freedomfighter.readersmotivation.data.Habit
import com.freedomfighter.readersmotivation.data.Kind
import com.freedomfighter.readersmotivation.data.Palettes
import com.freedomfighter.readersmotivation.data.Store
import com.freedomfighter.readersmotivation.data.TextSize
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

sealed class Screen {
    data object Home : Screen()
    data class Habit(val id: String) : Screen()
    /** null = a new habit. */
    data class Edit(val id: String?) : Screen()
    data class Urge(val id: String) : Screen()
    data class Slip(val id: String) : Screen()
    data class Journal(val id: String) : Screen()
    data object Settings : Screen()
}

class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop() { if (stack.size > 1) stack.removeAt(stack.size - 1) }
    /** Back to the habit's page, whatever was opened over it. */
    fun popTo(id: String) { while (stack.size > 1 && stack.last() != Screen.Habit(id)) stack.removeAt(stack.size - 1) }
}

/** Today, read again when the app comes back after midnight. */
@Composable
fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); val t = LocalDate.now(); if (t != today) today = t } }
    return today
}

// ---------------------------------------------------------------------------------------------
// Home: the habits, one row each; or the few words of the first time.
// ---------------------------------------------------------------------------------------------

@Composable
fun HomeScreen(nav: Nav, app: App, activity: MainActivity) {
    val context = LocalContext.current
    val colors = LocalColors.current
    val typo = LocalTypo.current
    val tick = rememberTick()
    val habits by app.store.habits.collectAsState()
    val today = rememberToday()
    var menu by remember { mutableStateOf(false) }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.app_title), onBack = null, trailing = "⋯", onTrailing = { menu = true })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (habits.isEmpty()) {
                    T(stringResource(R.string.no_habits), Modifier.padding(horizontal = rowPadH).padding(top = 22.dp, bottom = 8.dp), size = typo.tile * 1.3f, align = TextAlign.Start)
                    Small(stringResource(R.string.intro_text), Modifier.padding(horizontal = rowPadH).padding(bottom = 16.dp), maxLines = 14, align = TextAlign.Start)
                    Rule()
                } else {
                    VSpace(6.dp)
                    habits.forEach { h ->
                        val s = remember(h, today) { Counts.stats(h, today) }
                        TextRow(h.name, secondary = Words.summary(context, h, s)) { tick(); nav.push(Screen.Habit(h.id)) }
                    }
                    Rule()
                }
                TextRow(stringResource(R.string.new_habit), size = typo.title) { tick(); nav.push(Screen.Edit(null)) }
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        if (menu) TextMenu(null, emptyList(), onDismiss = { menu = false }, footer = listOf(
            MenuItem(Words.themeName(context, Palettes.next(app.prefs.settings.value.theme))) { app.prefs.nextTheme() },
            MenuItem(stringResource(R.string.settings)) { nav.push(Screen.Settings) }
        ))
    }
}

// ---------------------------------------------------------------------------------------------
// One habit: the day, what stays earned, the step, the reason, the two gestures.
// ---------------------------------------------------------------------------------------------

@Composable
fun HabitScreen(nav: Nav, app: App, activity: MainActivity, id: String) {
    val context = LocalContext.current
    val typo = LocalTypo.current
    val colors = LocalColors.current
    val tick = rememberTick()
    val habits by app.store.habits.collectAsState()
    val h = habits.firstOrNull { it.id == id }
    val today = rememberToday()
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(flash) { if (flash) { delay(700); flash = false } }
    BackHandler { nav.pop() }
    if (h == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    val s = remember(h, today) { Counts.stats(h, today) }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(h.name, onBack = { nav.pop() }, trailing = "⋯", onTrailing = { menu = true })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().padding(horizontal = rowPadH).padding(top = 22.dp, bottom = 12.dp)) {
                    Small(Words.since(context, h, today), maxLines = 3)
                    if (s.daysTotal > 0) {
                        T(Words.dayLine(context, s, h), size = typo.big, maxLines = 2, lineHeightMul = 1.1f)
                        Words.heldLine(context, h, s)?.let { Small(it, Modifier.padding(top = 4.dp), maxLines = 2) }
                        Words.restartLine(context, s, h)?.let { Small(it, Modifier.padding(top = 4.dp), maxLines = 2) }
                        if (h.kind == Kind.STOP) T(Words.resistedLine(context, s), Modifier.padding(top = 10.dp), size = typo.title, maxLines = 2)
                        Words.unitsLine(context, h, s)?.let { T(it, Modifier.padding(top = 4.dp), size = typo.title, maxLines = 2) }
                        Words.stepLine(context, s)?.let { Small(it, Modifier.padding(top = 10.dp), maxLines = 2) }
                        Words.stepNote(context, s)?.let { Small(it, Modifier.padding(top = 4.dp), maxLines = 5) }
                    }
                }
                if (h.why.isNotBlank()) {
                    Rule()
                    T(h.why, Modifier.padding(horizontal = rowPadH, vertical = 12.dp), size = typo.title, maxLines = 6)
                }
                Rule()
                // ---- the two gestures ----
                if (h.kind == Kind.STOP) {
                    TextRow(stringResource(R.string.urge_now), inverted = true) { tick(); nav.push(Screen.Urge(h.id)) }
                    TextRow(stringResource(R.string.resist_row), secondary = stringResource(R.string.resist_hint), inverted = flash) {
                        tick(); flash = true; app.store.add(h.id, Event(System.currentTimeMillis(), EventType.RESIST))
                    }
                } else {
                    if (s.doneToday) TextRow(stringResource(R.string.done_already), inverted = true)
                    else TextRow(stringResource(R.string.done_row), inverted = flash) { tick(); flash = true; app.store.add(h.id, Event(System.currentTimeMillis(), EventType.DONE)) }
                }
                Rule()
                // ---- the last weeks ----
                Column(Modifier.fillMaxWidth().padding(horizontal = rowPadH, vertical = 12.dp)) {
                    Small(stringResource(R.string.last_weeks), maxLines = 1)
                    Words.gridRows(h, today).forEach { T(it, Modifier.padding(top = 2.dp), size = typo.title, maxLines = 1, softWrap = false) }
                    Small(stringResource(if (h.kind == Kind.STOP) R.string.legend_stop else R.string.legend_do), Modifier.padding(top = 4.dp), maxLines = 1)
                }
                Rule()
                TextRow(stringResource(R.string.share_progress), secondary = stringResource(R.string.share_hint), size = typo.title) { Exports.shareText(context, Words.shareText(context, h, s, today)) }
                if (s.daysTotal > 0) TextRow(stringResource(R.string.slip_row), size = typo.title) { tick(); nav.push(Screen.Slip(h.id)) }
                VSpace(12.dp)
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        if (menu) TextMenu(h.name, listOf(
            MenuItem(stringResource(R.string.journal)) { nav.push(Screen.Journal(h.id)) },
            MenuItem(stringResource(R.string.edit)) { nav.push(Screen.Edit(h.id)) },
            MenuItem(stringResource(R.string.delete_habit)) { confirmDelete = true }
        ), onDismiss = { menu = false }, footer = listOf(
            MenuItem(Words.themeName(context, Palettes.next(app.prefs.settings.value.theme))) { app.prefs.nextTheme() },
            MenuItem(stringResource(R.string.settings)) { nav.push(Screen.Settings) }
        ))
        if (confirmDelete) TextMenu(stringResource(R.string.delete_q, h.name), listOf(
            MenuItem(stringResource(R.string.delete_yes)) { app.store.delete(h.id); nav.pop() },
            MenuItem(stringResource(R.string.action_cancel)) { }
        ), onDismiss = { confirmDelete = false }, titleLines = 3)
    }
}

// ---------------------------------------------------------------------------------------------
// An urge: three minutes with the ring, the reason and the plan; then "it passed" or "I slipped".
// ---------------------------------------------------------------------------------------------

private const val URGE_SECONDS = 180
private val PHASES = listOf(3 to R.string.breathe_in, 3 to R.string.breathe_hold, 6 to R.string.breathe_out)

@Composable
fun UrgeScreen(nav: Nav, app: App, id: String) {
    val context = LocalContext.current
    val typo = LocalTypo.current
    val colors = LocalColors.current
    val tick = rememberTick()
    val h = app.store.get(id)
    var elapsedMs by remember { mutableStateOf(0L) }
    val startedAt = remember { System.currentTimeMillis() }
    LaunchedEffect(Unit) { while (elapsedMs < URGE_SECONDS * 1000L) { delay(50); elapsedMs = System.currentTimeMillis() - startedAt } }
    BackHandler { nav.pop() }
    if (h == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    val over = elapsedMs >= URGE_SECONDS * 1000L
    // the breath: 3 s in, 3 s hold, 6 s out, over and over
    val cycle = PHASES.sumOf { it.first } * 1000L
    val inCycle = elapsedMs % cycle
    var acc = 0L; var phase = 0; var phaseFrac = 0f
    for ((i, p) in PHASES.withIndex()) {
        val len = p.first * 1000L
        if (inCycle < acc + len) { phase = i; phaseFrac = (inCycle - acc).toFloat() / len; break }
        acc += len
    }
    val radius = when (phase) { 0 -> 0.35f + 0.55f * phaseFrac; 1 -> 0.9f; else -> 0.9f - 0.55f * phaseFrac }
    val left = ((URGE_SECONDS * 1000L - elapsedMs).coerceAtLeast(0) / 1000L).toInt()
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.urge_title), onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                T(stringResource(if (over) R.string.urge_done_text else R.string.urge_text), Modifier.padding(horizontal = rowPadH).padding(top = 18.dp, bottom = 8.dp), size = typo.title, maxLines = 6)
                if (!over) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = rowPadH * 2).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        val fg = colors.fg
                        Canvas(Modifier.fillMaxSize()) {
                            val r = size.minDimension / 2f
                            drawCircle(fg.copy(alpha = 0.25f), r * 0.9f, Offset(r, r), style = Stroke(width = 2.dp.toPx()))
                            drawCircle(fg, r * radius, Offset(r, r), style = Stroke(width = 6.dp.toPx()))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            T(stringResource(PHASES[phase].second), size = typo.title, align = TextAlign.Center)
                            T("%d:%02d".format(left / 60, left % 60), size = typo.tile, color = colors.dim, align = TextAlign.Center)
                        }
                    }
                }
                if (h.why.isNotBlank()) { Rule(); T(h.why, Modifier.padding(horizontal = rowPadH, vertical = 12.dp), size = typo.title, maxLines = 6) }
                if (h.plan.isNotBlank()) { Rule(); T(stringResource(R.string.slip_plan, h.plan), Modifier.padding(horizontal = rowPadH, vertical = 12.dp), size = typo.title, maxLines = 6) }
                Rule()
                TextRow(stringResource(R.string.urge_passed), secondary = stringResource(R.string.urge_passed_hint), inverted = true) {
                    tick(); app.store.add(h.id, Event(System.currentTimeMillis(), EventType.RESIST)); nav.pop()
                }
                TextRow(stringResource(R.string.urge_still), size = typo.title) { tick(); nav.pop(); nav.push(Screen.Slip(h.id)) }
                VSpace(12.dp)
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

// ---------------------------------------------------------------------------------------------
// A slip: the facts that stay, the plan, a note if wanted, and the restart.
// ---------------------------------------------------------------------------------------------

@Composable
fun SlipScreen(nav: Nav, app: App, id: String) {
    val context = LocalContext.current
    val typo = LocalTypo.current
    val tick = rememberTick()
    val h = app.store.get(id)
    val today = rememberToday()
    var note by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf(false) }
    BackHandler { nav.pop() }
    if (h == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    val s = remember(h, today) { Counts.stats(h, today) }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.slip_title), onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                // what will stay once today is written as a slip (today was counted as held so far)
                val slippedToday = h.events.any { it.type == EventType.SLIP && it.date() == today }
                val heldAfter = if (h.kind == Kind.STOP && !slippedToday) (s.daysHeld - 1).coerceAtLeast(0) else s.daysHeld
                val text = if (h.kind == Kind.STOP) stringResource(R.string.slip_text, Words.heldStay(context, heldAfter), Words.urgesStay(context, s.resisted))
                    else stringResource(R.string.slip_text_do, Words.doneStay(context, s.daysHeld))
                T(text, Modifier.padding(horizontal = rowPadH).padding(top = 18.dp, bottom = 12.dp), size = typo.title, maxLines = 8)
                if (h.plan.isNotBlank()) { Rule(); T(stringResource(R.string.slip_plan, h.plan), Modifier.padding(horizontal = rowPadH, vertical = 12.dp), size = typo.title, maxLines = 6) }
                Rule()
                // a habit to do writes nothing (the day is simply not marked), so there is nothing to note
                if (h.kind == Kind.STOP) { TextRow(note.ifBlank { stringResource(R.string.slip_note_row) }, size = typo.title) { prompt = true }; Rule() }
                TextRow(stringResource(R.string.slip_confirm), inverted = true, size = typo.title) {
                    tick()
                    if (h.kind == Kind.STOP) app.store.add(h.id, Event(System.currentTimeMillis(), EventType.SLIP, note.trim()))
                    nav.popTo(h.id)
                }
                TextRow(stringResource(R.string.action_cancel), size = typo.title) { nav.pop() }
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        if (prompt) TextPrompt(stringResource(R.string.slip_note_prompt), initial = note, onDone = { note = it; prompt = false }, onCancel = { prompt = false })
    }
}

// ---------------------------------------------------------------------------------------------
// The record: every entry, newest first; a tap removes a wrong one.
// ---------------------------------------------------------------------------------------------

@Composable
fun JournalScreen(nav: Nav, app: App, id: String) {
    val typo = LocalTypo.current
    val habits by app.store.habits.collectAsState()
    val h = habits.firstOrNull { it.id == id }
    var selected by remember { mutableStateOf<Event?>(null) }
    val fmt = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(Locale.getDefault()) }
    BackHandler { nav.pop() }
    if (h == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.journal), onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                if (h.events.isEmpty()) Small(stringResource(R.string.journal_empty), Modifier.padding(horizontal = rowPadH, vertical = 14.dp))
                h.events.sortedByDescending { it.t }.forEach { e ->
                    val label = stringResource(when (e.type) { EventType.RESIST -> R.string.ev_resist; EventType.SLIP -> R.string.ev_slip; EventType.DONE -> R.string.ev_done })
                    val when_ = java.time.Instant.ofEpochMilli(e.t).atZone(java.time.ZoneId.systemDefault()).format(fmt)
                    TextRow(if (e.note.isBlank()) label else "$label · ${e.note}", secondary = when_, size = typo.title) { selected = e }
                }
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        selected?.let { e ->
            TextMenu(java.time.Instant.ofEpochMilli(e.t).atZone(java.time.ZoneId.systemDefault()).format(fmt), listOf(
                MenuItem(stringResource(R.string.remove_entry)) { app.store.removeEvent(h.id, e) }
            ), onDismiss = { selected = null })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// New or changed habit: one row per field, each opening its prompt.
// ---------------------------------------------------------------------------------------------

private enum class Field { NAME, START, WHY, PLAN, PER_DAY, UNIT, COST, CURRENCY }

@Composable
fun EditScreen(nav: Nav, app: App, id: String?) {
    val context = LocalContext.current
    val typo = LocalTypo.current
    val tick = rememberTick()
    val existing = remember(id) { id?.let { app.store.get(it) } }
    var draft by remember { mutableStateOf(existing ?: Habit(Store.newId(), "", Kind.STOP, LocalDate.now())) }
    var prompt by remember { mutableStateOf<Field?>(null) }
    var suggestions by remember { mutableStateOf(existing == null) }
    var startMenu by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    BackHandler { nav.pop() }
    val notCounted = stringResource(R.string.not_counted)
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(if (existing == null) stringResource(R.string.new_habit) else existing.name, onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                VSpace(6.dp)
                TextRow(stringResource(if (draft.kind == Kind.STOP) R.string.kind_stop else R.string.kind_do), secondary = stringResource(R.string.kind_title)) {
                    tick(); draft = draft.copy(kind = if (draft.kind == Kind.STOP) Kind.DO else Kind.STOP)
                }
                TextRow(draft.name.ifBlank { "…" }, secondary = stringResource(R.string.name_title)) { if (existing == null && draft.name.isBlank() && draft.kind == Kind.STOP) suggestions = true else prompt = Field.NAME }
                TextRow(Words.date(draft.start), secondary = stringResource(R.string.start_title)) { startMenu = true }
                Small(stringResource(R.string.start_hint), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 5)
                Rule(Modifier.padding(vertical = 6.dp))
                TextRow(draft.why.ifBlank { "…" }, secondary = stringResource(R.string.why_title)) { prompt = Field.WHY }
                Small(stringResource(R.string.why_hint), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 3)
                TextRow(draft.plan.ifBlank { "…" }, secondary = stringResource(R.string.plan_title)) { prompt = Field.PLAN }
                Small(stringResource(R.string.plan_hint), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 3)
                if (draft.kind == Kind.STOP) {
                    Rule(Modifier.padding(vertical = 6.dp))
                    TextRow(if (draft.unitsPerDay > 0) Words.number(draft.unitsPerDay) else notCounted, secondary = stringResource(R.string.per_day_title)) { prompt = Field.PER_DAY }
                    if (draft.unitsPerDay > 0) {
                        TextRow(draft.unitName.ifBlank { "…" }, secondary = stringResource(R.string.unit_title)) { prompt = Field.UNIT }
                        TextRow(if (draft.costPerUnit > 0) Words.number(draft.costPerUnit) else notCounted, secondary = stringResource(R.string.cost_title)) { prompt = Field.COST }
                        if (draft.costPerUnit > 0) TextRow(draft.currency.ifBlank { "…" }, secondary = stringResource(R.string.currency_title)) { prompt = Field.CURRENCY }
                    }
                    Small(stringResource(R.string.units_hint), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 3)
                }
                Rule(Modifier.padding(vertical = 6.dp))
                TextRow(stringResource(R.string.save), inverted = draft.name.isNotBlank()) {
                    if (draft.name.isNotBlank()) {
                        tick()
                        val h = draft.copy(name = draft.name.trim(), why = draft.why.trim(), plan = draft.plan.trim(), unitName = draft.unitName.trim(), currency = draft.currency.trim())
                        app.store.save(h)
                        nav.pop()
                        if (existing == null) nav.push(Screen.Habit(h.id))
                    }
                }
                VSpace(12.dp)
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        if (suggestions) {
            val names = listOf(R.string.sugg_sugar, R.string.sugg_cigarettes, R.string.sugg_alcohol, R.string.sugg_screens).map { stringResource(it) }
            TextMenu(stringResource(R.string.name_prompt_stop), names.map { n -> MenuItem(n) { draft = draft.copy(name = n) } } + MenuItem(stringResource(R.string.sugg_other)) { prompt = Field.NAME }, onDismiss = { suggestions = false })
        }
        if (startMenu) {
            val monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            val first = today.with(TemporalAdjusters.firstDayOfNextMonth())
            TextMenu(stringResource(R.string.start_title), listOf(
                MenuItem(stringResource(R.string.start_today), secondary = Words.date(today)) { draft = draft.copy(start = today) },
                MenuItem(stringResource(R.string.start_monday), secondary = Words.date(monday)) { draft = draft.copy(start = monday) },
                MenuItem(stringResource(R.string.start_first), secondary = Words.date(first)) { draft = draft.copy(start = first) },
                MenuItem(stringResource(R.string.start_date)) { prompt = Field.START }
            ), onDismiss = { startMenu = false })
        }
        prompt?.let { f ->
            val (title, initial, keyboard) = when (f) {
                Field.NAME -> Triple(stringResource(if (draft.kind == Kind.STOP) R.string.name_prompt_stop else R.string.name_prompt_do), draft.name, KeyboardType.Text)
                Field.START -> Triple(stringResource(R.string.start_date_prompt), draft.start.toString(), KeyboardType.Number)
                Field.WHY -> Triple(stringResource(R.string.why_prompt), draft.why, KeyboardType.Text)
                Field.PLAN -> Triple(stringResource(R.string.plan_prompt), draft.plan, KeyboardType.Text)
                Field.PER_DAY -> Triple(stringResource(R.string.per_day_prompt), if (draft.unitsPerDay > 0) Words.number(draft.unitsPerDay) else "", KeyboardType.Decimal)
                Field.UNIT -> Triple(stringResource(R.string.unit_prompt), draft.unitName, KeyboardType.Text)
                Field.COST -> Triple(stringResource(R.string.cost_prompt), if (draft.costPerUnit > 0) Words.number(draft.costPerUnit) else "", KeyboardType.Decimal)
                Field.CURRENCY -> Triple(stringResource(R.string.currency_prompt), draft.currency, KeyboardType.Text)
            }
            TextPrompt(title, initial = initial, selectAll = initial.isNotEmpty(), keyboard = keyboard, onDone = { v ->
                val num = v.replace(',', '.').trim().toDoubleOrNull() ?: 0.0
                draft = when (f) {
                    Field.NAME -> draft.copy(name = v)
                    Field.START -> runCatching { LocalDate.parse(v.trim()) }.getOrNull()?.let { draft.copy(start = it) } ?: draft
                    Field.WHY -> draft.copy(why = v)
                    Field.PLAN -> draft.copy(plan = v)
                    Field.PER_DAY -> draft.copy(unitsPerDay = num)
                    Field.UNIT -> draft.copy(unitName = v)
                    Field.COST -> draft.copy(costPerUnit = num)
                    Field.CURRENCY -> draft.copy(currency = v)
                }
                prompt = null
            }, onCancel = { prompt = null })
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings: the reminder, the widget's habit, the backup, the look.
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(nav: Nav, app: App, activity: MainActivity) {
    val context = LocalContext.current
    val s by app.prefs.settings.collectAsState()
    val habits by app.store.habits.collectAsState()
    val colors = LocalColors.current
    val typo = LocalTypo.current
    var reminderMenu by remember { mutableStateOf(false) }
    var timePrompt by remember { mutableStateOf(false) }
    var habitMenu by remember { mutableStateOf(false) }
    var why by remember { mutableStateOf(false) }
    val saved by activity.savedExport
    val pending by activity.pendingImport
    BackHandler { nav.pop() }
    fun setReminder(m: Int) { app.prefs.setReminderMinutes(m); Reminder.schedule(context); if (m >= 0) activity.askNotificationPermission() }
    Page {
        Column(Modifier.fillMaxSize()) {
            ScreenTitle(stringResource(R.string.settings), onBack = { nav.pop() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                VSpace(10.dp)
                TextRow(if (s.reminderMinutes < 0) stringResource(R.string.off) else Words.minutesWord(s.reminderMinutes), secondary = stringResource(R.string.reminder_title)) { reminderMenu = true }
                Small(stringResource(R.string.reminder_hint), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 6)
                if (habits.size > 1) {
                    TextRow(app.featured()?.name ?: "", secondary = stringResource(R.string.widget_habit_title)) { habitMenu = true }
                }
                Rule(Modifier.padding(vertical = 8.dp))
                TextRow(stringResource(R.string.export_backup), size = typo.title) { activity.exportBackup() }
                TextRow(stringResource(R.string.import_backup), size = typo.title) { activity.importBackup() }
                Rule(Modifier.padding(vertical = 8.dp))
                TextRow(Words.themeName(context, s.theme), secondary = stringResource(R.string.colours)) { app.prefs.nextTheme() }
                TextRow(when (s.textSize) { TextSize.SMALL -> stringResource(R.string.size_small); TextSize.MEDIUM -> stringResource(R.string.size_medium); TextSize.LARGE -> stringResource(R.string.size_large) }, secondary = stringResource(R.string.text_size)) {
                    app.prefs.setTextSize(when (s.textSize) { TextSize.SMALL -> TextSize.MEDIUM; TextSize.MEDIUM -> TextSize.LARGE; TextSize.LARGE -> TextSize.SMALL })
                }
                TextRow(when (s.font) { FontChoice.SANS -> stringResource(R.string.font_sans); FontChoice.SERIF -> stringResource(R.string.font_serif); FontChoice.MONO -> stringResource(R.string.font_mono) }, secondary = stringResource(R.string.font)) {
                    app.prefs.setFont(when (s.font) { FontChoice.SANS -> FontChoice.SERIF; FontChoice.SERIF -> FontChoice.MONO; FontChoice.MONO -> FontChoice.SANS })
                }
                TextRow(if (s.haptics) stringResource(R.string.on) else stringResource(R.string.off), secondary = stringResource(R.string.haptics)) { app.prefs.setHaptics(!s.haptics) }
                Rule(Modifier.padding(vertical = 8.dp))
                TextRow(stringResource(R.string.why_it_works), size = typo.title) { why = !why }
                if (why) Small(stringResource(R.string.why_it_works_text), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 30, align = TextAlign.Start)
                TextRow(stringResource(R.string.app_name), secondary = stringResource(R.string.about), size = typo.title) { }
                Small(stringResource(R.string.credits), Modifier.padding(horizontal = rowPadH).padding(bottom = 10.dp), maxLines = 3)
            }
            Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
        if (reminderMenu) TextMenu(stringResource(R.string.reminder_title), listOf(
            MenuItem(stringResource(R.string.off)) { setReminder(-1) },
            MenuItem("08:00") { setReminder(8 * 60) },
            MenuItem("12:00") { setReminder(12 * 60) },
            MenuItem("20:00") { setReminder(20 * 60) },
            MenuItem("21:00") { setReminder(21 * 60) },
            MenuItem(stringResource(R.string.time_prompt)) { timePrompt = true }
        ), onDismiss = { reminderMenu = false })
        if (timePrompt) TextPrompt(stringResource(R.string.time_prompt), initial = if (s.reminderMinutes >= 0) Words.minutesWord(s.reminderMinutes) else "", selectAll = true, keyboard = KeyboardType.Number,
            onDone = { t -> Words.parseTime(t)?.let { setReminder(it) }; timePrompt = false }, onCancel = { timePrompt = false })
        if (habitMenu) TextMenu(stringResource(R.string.widget_habit_title), habits.map { h -> MenuItem(h.name) { app.prefs.setWidgetHabit(h.id); com.freedomfighter.readersmotivation.widget.MotivationWidget.refresh(context) } }, onDismiss = { habitMenu = false })
        if (saved) TextMenu(stringResource(R.string.export_saved, Exports.NAME), listOf(MenuItem(stringResource(R.string.share_export)) { activity.shareBackup() }), onDismiss = { activity.savedExport.value = false }, titleLines = 2)
        pending?.let { text ->
            TextMenu(stringResource(R.string.import_q), listOf(
                MenuItem(stringResource(R.string.replace)) { if (app.store.import(text)) activity.toast(context.getString(R.string.import_done)) else activity.toast(context.getString(R.string.import_failed)) },
                MenuItem(stringResource(R.string.action_cancel)) { }
            ), onDismiss = { activity.pendingImport.value = null }, titleLines = 2)
        }
    }
}
