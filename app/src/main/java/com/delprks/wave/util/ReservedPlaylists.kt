package com.delprks.wave.util

object ReservedPlaylists {
    const val DOWNLOADS_PLAYLIST_ID = "downloads"
    const val LOVED_TRACKS_PLAYLIST_ID = "loved_tracks"

    private val RESERVED_PLAYLIST_IDS = listOf(DOWNLOADS_PLAYLIST_ID, LOVED_TRACKS_PLAYLIST_ID)

    fun isReserved(name: String): Boolean {
        return RESERVED_PLAYLIST_IDS.contains(name)
    }
}
