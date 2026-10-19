package com.freedomfighter.readersmotivation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.freedomfighter.readersmotivation.data.Exports
import com.freedomfighter.readersmotivation.ui.EditScreen
import com.freedomfighter.readersmotivation.ui.HabitScreen
import com.freedomfighter.readersmotivation.ui.HomeScreen
import com.freedomfighter.readersmotivation.ui.JournalScreen
import com.freedomfighter.readersmotivation.ui.LocalColors
import com.freedomfighter.readersmotivation.ui.Nav
import com.freedomfighter.readersmotivation.ui.ReaderTheme
import com.freedomfighter.readersmotivation.ui.Screen
import com.freedomfighter.readersmotivation.ui.SettingsScreen
import com.freedomfighter.readersmotivation.ui.SlipScreen
import com.freedomfighter.readersmotivation.ui.UrgeScreen

class MainActivity : ComponentActivity() {
    private val nav = Nav()
    /** Set once a backup is saved: the "share this file" menu is offered. */
    val savedExport = mutableStateOf(false)
    /** The text of a backup picked for import, waiting for the person's yes. */
    val pendingImport = mutableStateOf<String?>(null)

    private val writeBackup = registerForActivityResult(ActivityResultContracts.CreateDocument(Exports.MIME)) { uri ->
        if (uri != null && Exports.save(this, uri, (application as App).store.export())) savedExport.value = true
    }
    private val readBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        val text = Exports.read(this, uri)
        if (text == null || runCatching { com.freedomfighter.readersmotivation.data.Store.parse(text) }.isFailure) toast(getString(R.string.import_failed))
        else pendingImport.value = text
    }
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun exportBackup() = writeBackup.launch(Exports.NAME)
    fun importBackup() = readBackup.launch(arrayOf("application/json", "text/plain", "*/*"))
    fun shareBackup() = Exports.share(this, getString(R.string.share_export))
    fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    fun askNotificationPermission() { if (Build.VERSION.SDK_INT >= 33 && !Reminder.allowed(this)) askNotifications.launch(android.Manifest.permission.POST_NOTIFICATIONS) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as App
        // one habit only: the app opens on it, the list is one tap back
        if (savedInstanceState == null) app.store.habits.value.singleOrNull()?.let { nav.push(Screen.Habit(it.id)) }
        setContent {
            val settings by app.prefs.settings.collectAsState()
            ReaderTheme(settings) {
                Bars()
                when (val s = nav.current) {
                    Screen.Home -> HomeScreen(nav, app, this)
                    is Screen.Habit -> HabitScreen(nav, app, this, s.id)
                    is Screen.Edit -> EditScreen(nav, app, s.id)
                    is Screen.Urge -> UrgeScreen(nav, app, s.id)
                    is Screen.Slip -> SlipScreen(nav, app, s.id)
                    is Screen.Journal -> JournalScreen(nav, app, s.id)
                    Screen.Settings -> SettingsScreen(nav, app, this)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        com.freedomfighter.readersmotivation.widget.MotivationWidget.refresh(this)
    }

    @Composable
    private fun Bars() {
        val view = LocalView.current
        val colors = LocalColors.current
        LaunchedEffect(colors.isDark) {
            val c = WindowInsetsControllerCompat(window, view)
            c.isAppearanceLightStatusBars = !colors.isDark
            c.isAppearanceLightNavigationBars = !colors.isDark
        }
    }
}
