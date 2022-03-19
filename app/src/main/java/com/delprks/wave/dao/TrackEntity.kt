package com.delprks.wave.dao

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.delprks.wave.domain.ContainerLocation
import java.util.*

@Entity
data class TrackEntity(
    @PrimaryKey @ColumnInfo(name = "track_id") var trackId: String,
    var title : String,
    var path: String,
    var location: ContainerLocation,
    var imageByteArray: ByteArray?,
    var imageBitmapUri: String?,
    var artist: String?,
    var loved: Boolean,
    val genre: String?,
    val duration: Long?,
    val created: Date?,
    val modified: Date?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackEntity

        if (trackId != other.trackId) return false
        if (title != other.title) return false
        if (path != other.path) return false
        if (location != other.location) return false
        if (imageByteArray != null) {
            if (other.imageByteArray == null) return false
            if (!imageByteArray.contentEquals(other.imageByteArray)) return false
        } else if (other.imageByteArray != null) return false
        if (imageBitmapUri != other.imageBitmapUri) return false
        if (artist != other.artist) return false
        if (loved != other.loved) return false
        if (genre != other.genre) return false
        if (duration != other.duration) return false
        if (created != other.created) return false
        if (modified != other.modified) return false

        return true
    }

    override fun hashCode(): Int {
        var result = trackId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
        result = 31 * result + (imageBitmapUri?.hashCode() ?: 0)
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + loved.hashCode()
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        result = 31 * result + (modified?.hashCode() ?: 0)
        return result
    }
}