package com.delprks.wave.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TrackStatusEntity(
    @PrimaryKey var id: String = "latest",
    var trackPosition: Int,
    var trackProgress: Long,
    var shuffled: Boolean,
    var playlistId: String?
)
