package com.delprks.wave.dao

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithTracksEntity(
    @Embedded
    val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlist_id",
        entityColumn = "track_id",
        associateBy = Junction(PlaylistTrackEntity::class)
    )
    val tracks: List<TrackEntity>
)
