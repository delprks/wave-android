package com.delprks.wave.domain

import android.graphics.Bitmap
import android.net.Uri
import java.util.*

data class TrackContainer(
    override var id: String,
    override var name: String,
    override var path: String,
    override var location: ContainerLocation,
    override var order: Int,
    var image: Bitmap?,
    var imageByteArray: ByteArray?,
    var imageBitmapUri: Uri?,
    var artist: String?,
    var loved: Boolean = false,
    val genre: String?,
    val duration: Long?,
    val created: Date?,
    val modified: Date?
) : Container(id, name, path, ContainerType.FILE, location, order) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrackContainer

        if (id != other.id) return false
        if (name != other.name) return false
        if (path != other.path) return false
        if (location != other.location) return false
        if (order != other.order) return false
        if (image != other.image) return false
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
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + order
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
        result = 31 * result + (imageBitmapUri?.hashCode() ?: 0)
        result = 31 * result + (artist?.hashCode() ?: 0)
        result = 31 * result + loved.hashCode()
        result = 31 * result + (genre?.hashCode() ?: 0)
        result = 31 * result + (duration?.hashCode() ?: 0)
        result = 31 * result + (created?.hashCode() ?: 0)
        result = 31 * result + modified.hashCode()
        return result
    }
}