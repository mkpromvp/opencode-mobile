package com.opencode.mobile.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.formatTimestamp(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) this else "${this.take(maxLength)}..."
}
