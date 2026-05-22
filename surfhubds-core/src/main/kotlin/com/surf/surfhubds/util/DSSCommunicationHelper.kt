package com.surf.surfhubds.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Port do `DSSCommunicationHelper.swift` — links, ligação telefônica e WhatsApp.
 */
object DSSCommunicationHelper {

    fun openLink(context: Context, urlString: String) {
        val uri = Uri.parse(urlString)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { /* ignore */ }
    }

    fun caller(context: Context, number: String) {
        val cleaned = number.filter { it.isDigit() || it == '*' || it == '#' }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try { context.startActivity(intent) } catch (e: ActivityNotFoundException) { /* ignore */ }
    }

    fun openWhatsApp(context: Context, number: String, message: String = "") {
        val encoded = Uri.encode(message)
        // Tenta abrir o app nativo primeiro
        val nativeUri = Uri.parse("whatsapp://send?phone=$number&text=$encoded")
        val nativeIntent = Intent(Intent.ACTION_VIEW, nativeUri).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(nativeIntent)
            return
        } catch (e: ActivityNotFoundException) {
            // Fallback web
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://api.whatsapp.com/send?phone=$number&text=$encoded"),
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            try { context.startActivity(webIntent) } catch (e: ActivityNotFoundException) { /* ignore */ }
        }
    }
}
