package com.delprks.wave.domain

import java.util.*

data class Playlist(
    var id: String,
    var name : String,
    var size: Int,
    var image: String?,
    var tracks: List<TrackContainer>,
    var order: Int,
    var favourite: Boolean = false,
    var duration: Long = 0,
    var created: Date,
    var modified: Date
    )
