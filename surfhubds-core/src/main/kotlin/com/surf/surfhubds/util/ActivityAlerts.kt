package com.surf.surfhubds.util

import android.content.Context
import androidx.appcompat.app.AlertDialog

/**
 * Port das extensions `UIViewController+Alerts.swift`. No Android, extension functions
 * em [Context].
 */

fun Context.showSurfAlert(
    title: String,
    message: String,
    primaryButtonTitle: String = "OK",
    primaryAction: (() -> Unit)? = null,
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(primaryButtonTitle) { dialog, _ ->
            primaryAction?.invoke()
            dialog.dismiss()
        }
        .setCancelable(true)
        .show()
}

fun Context.showSurfAlert(
    title: String,
    message: String,
    primaryButtonTitle: String,
    primaryAction: () -> Unit,
    secondaryButtonTitle: String,
    secondaryAction: () -> Unit,
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(primaryButtonTitle) { dialog, _ ->
            primaryAction()
            dialog.dismiss()
        }
        .setNegativeButton(secondaryButtonTitle) { dialog, _ ->
            secondaryAction()
            dialog.dismiss()
        }
        .setCancelable(true)
        .show()
}
