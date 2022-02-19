package com.delprks.wave.dao

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["playlist_id", "track_id"])
data class PlaylistTrackEntity(
    @ColumnInfo(name = "playlist_id") var playlistId: String,
    @ColumnInfo(name = "track_id") var trackId: String,
    var position: Int
)
