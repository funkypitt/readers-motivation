package com.freedomfighter.readersmotivation.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * The backup: written where the person chose to save it, with a copy kept for the share menu
 * (Telegram, kDrive, Drive, e-mail…) offered right after.
 */
object Exports {
    const val NAME = "readers-motivation-backup.json"
    const val MIME = "application/json"

    private fun dir(context: Context) = File(context.cacheDir, "exports")

    fun cleanUp(context: Context) { dir(context).listFiles()?.forEach { it.delete() } }

    fun save(context: Context, uri: Uri, text: String): Boolean {
        cleanUp(context)
        val written = runCatching {
            // "wt": a shorter text written over a longer file must not leave the old tail behind
            context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
        if (written) File(dir(context).apply { mkdirs() }, NAME).writeText(text)
        return written
    }

    fun share(context: Context, chooserTitle: String) {
        val file = File(dir(context), NAME)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(context, context.packageName + ".exports", file)
        val send = Intent(Intent.ACTION_SEND).setType(MIME).putExtra(Intent.EXTRA_STREAM, uri).putExtra(Intent.EXTRA_SUBJECT, NAME).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(NAME, uri)
        runCatching { context.startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
    }

    fun read(context: Context, uri: Uri): String? = runCatching { context.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) } }.getOrNull()

    /** A line of text to whoever the person wants to tell. */
    fun shareText(context: Context, text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        runCatching { context.startActivity(Intent.createChooser(send, null)) }
    }
}
