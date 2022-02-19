package com.delprks.wave.util

object SupportedContent {
    private val SUPPORTED_CONTENT = listOf("mp3", "mp4")

    fun isValid(fileName: String): Boolean {
        return SUPPORTED_CONTENT.contains(fileName.substringAfterLast('.', "").lowercase())
    }
}
