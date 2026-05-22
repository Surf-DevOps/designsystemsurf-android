package com.surf.surfhubds.util

import android.content.Context
import android.util.TypedValue
import android.view.View

internal val View.density: Float get() = resources.displayMetrics.density

fun Float.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).toInt()

fun Float.dpToPxFloat(context: Context): Float =
    this * context.resources.displayMetrics.density

fun Int.dpToPxFloat(context: Context): Float =
    this * context.resources.displayMetrics.density

fun Float.spToPx(context: Context): Float = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics,
)
