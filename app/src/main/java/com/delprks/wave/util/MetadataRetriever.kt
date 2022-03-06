package com.delprks.wave.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.delprks.wave.App
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.security.SettingsManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

object MetadataRetriever {
    fun getTrackMetadata(path: String, location: ContainerLocation): TrackMetadata {
        val mediaMetadataRetriever = MediaMetadataRetriever()

        if (location == ContainerLocation.LOCAL) {
            val file = File(path)
            val fileInputStream = FileInputStream(file)
            val fd = fileInputStream.fd
            mediaMetadataRetriever.setDataSource(fd)
            fileInputStream.close()
        } else {
            mediaMetadataRetriever.setDataSource(path, SettingsManager.getAuthMap(App.applicationContext()))
        }

        val title = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
        val artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        val art = mediaMetadataRetriever.embeddedPicture
        val genre = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
        val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let { it.toLong() / 1000 } // ms to s
        val yearCreated = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toInt()

        return TrackMetadata(title, artist, null, art, genre, duration, yearCreated)
    }

    data class TrackMetadata(
        var title: String?,
        var artist: String?,
        var image: Bitmap?,
        var imageByteArray: ByteArray?,
        val genre: String?,
        val duration: Long?,
        val yearCreated: Int?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TrackMetadata

            if (title != other.title) return false
            if (artist != other.artist) return false
            if (image != other.image) return false
            if (imageByteArray != null) {
                if (other.imageByteArray == null) return false
                if (!imageByteArray.contentEquals(other.imageByteArray)) return false
            } else if (other.imageByteArray != null) return false
            if (genre != other.genre) return false
            if (duration != other.duration) return false
            if (yearCreated != other.yearCreated) return false

            return true
        }

        override fun hashCode(): Int {
            var result = title?.hashCode() ?: 0
            result = 31 * result + (artist?.hashCode() ?: 0)
            result = 31 * result + (image?.hashCode() ?: 0)
            result = 31 * result + (imageByteArray?.contentHashCode() ?: 0)
            result = 31 * result + (genre?.hashCode() ?: 0)
            result = 31 * result + (duration?.hashCode() ?: 0)
            result = 31 * result + (yearCreated ?: 0)
            return result
        }
    }

    fun byteToBitmap(image: ByteArray?): Bitmap? = image?.let { BitmapFactory.decodeByteArray(image, 0, it.size) }

    fun bitmapToByte(image: Bitmap?): ByteArray? {
        val stream = ByteArrayOutputStream()

        return image?.let { img ->
            img.compress(Bitmap.CompressFormat.PNG, 100, stream)

            stream.toByteArray()
        }
    }
}
