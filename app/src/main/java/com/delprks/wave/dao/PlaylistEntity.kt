package com.delprks.wave.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class PlaylistEntity(
    @PrimaryKey @ColumnInfo(name = "playlist_id") var playlistId: String,
    var name : String,
    var size: Int,
    var imageUri: String?,
    var order: Int = 0,
    var favourite: Boolean = false,
    var duration: Long = 0,
    var created: Date,
    var modified: Date
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlaylistEntity

        if (playlistId != other.playlistId) return false
        if (name != other.name) return false
        if (size != other.size) return false
        if (imageUri != null) {
            if (other.imageUri == null) return false
            if (!imageUri.contentEquals(other.imageUri)) return false
        } else if (other.imageUri != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = playlistId.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + size
        result = 31 * result + imageUri.hashCode()
        return result
    }
}
