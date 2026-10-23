package com.surf.surfhubds.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Port do `DSSCommunicationHelper.swift` — links, ligação telefônica, WhatsApp e loja de apps.
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

    /**
     * Abre a página do app na loja ("Avaliar na loja" da Jornada de Avaliação).
     *
     * Tenta, nesta ordem: [storeUrl] (URL da MVNO, ex.: `urlLoja` do `/config`), a Play Store
     * com o pacote fixo, qualquer app que trate `market://` e a página web da Play. Sem fixar
     * o pacote da Play, aparelhos Samsung abrem um seletor com a Galaxy Store. Sem [storeUrl],
     * usa o pacote do próprio app ([Context.getPackageName]).
     */
    fun openAppStore(context: Context, storeUrl: String? = null) {
        val packageName = context.packageName
        val market = Uri.parse("market://details?id=$packageName")
        val candidates = listOfNotNull(
            storeUrl?.takeIf { it.isNotBlank() }?.let { Intent(Intent.ACTION_VIEW, Uri.parse(it)) },
            Intent(Intent.ACTION_VIEW, market).setPackage(PLAY_STORE_PACKAGE),
            Intent(Intent.ACTION_VIEW, market),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")),
        )
        for (intent in candidates) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                context.startActivity(intent)
                return
            } catch (e: ActivityNotFoundException) {
                // Tenta o próximo: sem a Play instalada, o pacote fixo não abre.
            }
        }
    }

    private const val PLAY_STORE_PACKAGE = "com.android.vending"
}
