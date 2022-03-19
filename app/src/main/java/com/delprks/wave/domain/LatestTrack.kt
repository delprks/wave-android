package com.delprks.wave.domain

data class LatestTrack(
    var id: String = "latest",
    var trackPosition: Int,
    var trackProgress: Long,
    var shuffled: Boolean,
    var playlistId: String?
)
