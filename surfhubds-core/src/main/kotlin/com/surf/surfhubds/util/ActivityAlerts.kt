package com.surf.surfhubds.util

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Port das extensions `UIViewController+Alerts.swift`. No Android, extension functions
 * em [Context].
 *
 * Os alerts aparecem com um blur do fundo (snapshot borrado atrás do dialog), igual ao
 * iOS. O blur só é aplicado quando o [Context] é uma Activity.
 */

fun Context.showSurfAlert(
    title: String,
    message: String,
    primaryButtonTitle: String = "OK",
    primaryAction: (() -> Unit)? = null,
) {
    val dialog = AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(primaryButtonTitle) { d, _ ->
            primaryAction?.invoke()
            d.dismiss()
        }
        .setCancelable(true)
        .create()
    showWithBlur(dialog)
}

fun Context.showSurfAlert(
    title: String,
    message: String,
    primaryButtonTitle: String,
    primaryAction: () -> Unit,
    secondaryButtonTitle: String,
    secondaryAction: () -> Unit,
) {
    val dialog = AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(primaryButtonTitle) { d, _ ->
            primaryAction()
            d.dismiss()
        }
        .setNegativeButton(secondaryButtonTitle) { d, _ ->
            secondaryAction()
            d.dismiss()
        }
        .setCancelable(true)
        .create()
    showWithBlur(dialog)
}

/**
 * Mostra um [AlertDialog] com blur do fundo. Adiciona um backdrop borrado na Activity
 * por trás do dialog e o remove no dismiss. Sem Activity, mostra o alert normal.
 */
internal fun Context.showWithBlur(dialog: AlertDialog) {
    val activity = DSSBlur.activityOf(this)
    if (activity == null) {
        dialog.show()
        return
    }
    val backdrop = DSSBlur.addBlurBackdrop(activity)
    dialog.setOnDismissListener { DSSBlur.removeBackdrop(backdrop) }
    dialog.show()
    // O backdrop já tem o blur+scrim; zera o dim do alert p/ não escurecer duas vezes.
    dialog.window?.setDimAmount(0f)
}
