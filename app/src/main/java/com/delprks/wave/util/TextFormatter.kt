package com.delprks.wave.util

object TextFormatter {
    fun shorten(text: String, max: Int): String {
        return if (text.length >= max) {
            text.substring(0, max - 3) + "..."
        } else {
            text
        }
    }
}
