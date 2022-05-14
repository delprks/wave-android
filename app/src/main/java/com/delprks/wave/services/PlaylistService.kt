package com.delprks.wave.services

import android.net.Uri
import com.delprks.wave.App
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.domain.Playlist
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.dao.PlaylistEntity
import com.delprks.wave.dao.TrackEntity
import com.delprks.wave.dao.LatestTrackEntity
import com.delprks.wave.domain.LatestTrack
import com.delprks.wave.util.ImageCache
import com.delprks.wave.util.MetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object PlaylistService {
    suspend fun getAllPlaylistsWithoutTracks(db: AppDatabase): List<Playlist> {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            playlistDao.getAllPlaylistsWithoutTracks().map { playlistEntity ->
                Playlist(
                    playlistEntity.playlistId,
                    playlistEntity.name,
                    playlistEntity.size,
                    playlistEntity.imageUri,
                    listOf(),
                    playlistEntity.order,
                    playlistEntity.favourite,
                    playlistEntity.duration,
                    playlistEntity.created,
                    playlistEntity.modified
                )
            }
        }
    }

    suspend fun updateTrackStatus(db: AppDatabase, latestTrack: LatestTrack) {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val trackStatusEntity = LatestTrackEntity(
                latestTrack.id,
                latestTrack.trackPosition,
                latestTrack.trackProgress,
                latestTrack.shuffled,
                latestTrack.playlistId
            )

            playlistDao.updateLatestTrack(trackStatusEntity)
        }
    }

    suspend fun getLatestTrack(db: AppDatabase): LatestTrack? {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val trackStatus = playlistDao.getLatestTrack("latest")

            trackStatus?.let {
                LatestTrack(
                    trackStatus.id,
                    trackStatus.trackPosition,
                    trackStatus.trackProgress,
                    trackStatus.shuffled,
                    trackStatus.playlistId
                )
            }
        }
    }

    suspend fun getPlaylistWithoutTracks(db: AppDatabase, playlistId: String): Playlist {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val playlistEntity = playlistDao.getPlaylistWithoutTracks(playlistId)

            Playlist(
                playlistEntity.playlistId,
                playlistEntity.name,
                playlistEntity.size,
                playlistEntity.imageUri,
                listOf(),
                playlistEntity.order,
                playlistEntity.favourite,
                playlistEntity.duration,
                playlistEntity.created,
                playlistEntity.modified
            )
        }
    }


    suspend fun getPlaylistPopulatedWithTracks(db: AppDatabase, playlistId: String): Playlist {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val playlistWithTracks = playlistDao.getPlaylistPopulatedWithTracks(playlistId)

            var trackCount = 0

            Playlist(
                playlistWithTracks.playlist.playlistId,
                playlistWithTracks.playlist.name,
                playlistWithTracks.tracks.size,
                playlistWithTracks.playlist.imageUri,
                playlistWithTracks.tracks.sortedBy { it.title }.map { trackEntity ->
                    TrackContainer(
                        trackEntity.trackId,
                        trackEntity.title,
                        trackEntity.path,
                        trackEntity.location,
                        trackCount++,
                        //   MetadataRetriever.byteToBitmap(trackEntity.image),
                        null,
                        trackEntity.imageByteArray,
                        trackEntity.imageBitmapUri?.let { Uri.parse("${App.applicationContext().cacheDir}${trackEntity.imageBitmapUri}") },
                        trackEntity.artist,
                        trackEntity.loved,
                        trackEntity.genre,
                        trackEntity.duration,
                        trackEntity.created,
                        trackEntity.modified
                    )
                },
                playlistWithTracks.playlist.order,
                playlistWithTracks.playlist.favourite,
                playlistWithTracks.playlist.duration,
                playlistWithTracks.playlist.created,
                playlistWithTracks.playlist.modified
            )
        }
    }

    suspend fun getTracksByIds(db: AppDatabase, trackIds: List<String>): List<TrackContainer> {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            playlistDao.getTracksById(trackIds).map { trackEntity ->
                TrackContainer(
                    trackEntity.trackId,
                    trackEntity.title,
                    trackEntity.path,
                    trackEntity.location,
                    0, // TODO (dp-18.12.21) :: retrieve correct order
                    // MetadataRetriever.byteToBitmap(trackEntity.image),
                    null,
                    trackEntity.imageByteArray,
                    trackEntity.imageBitmapUri?.let { Uri.parse("${App.applicationContext().cacheDir}${trackEntity.imageBitmapUri}") },
                    trackEntity.artist,
                    trackEntity.loved,
                    trackEntity.genre,
                    trackEntity.duration,
                    trackEntity.created,
                    trackEntity.modified
                )
            }
        }
    }

    suspend fun getTrackById(db: AppDatabase, trackId: String): TrackContainer {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val trackEntity = playlistDao.getTrackById(trackId)
            TrackContainer(
                trackEntity.trackId,
                trackEntity.title,
                trackEntity.path,
                trackEntity.location,
                0, // TODO (dp-18.12.21) :: retrieve correct order
                // MetadataRetriever.byteToBitmap(trackEntity.image),
                null,
                trackEntity.imageByteArray,
                trackEntity.imageBitmapUri?.let { Uri.parse("${App.applicationContext().cacheDir}${trackEntity.imageBitmapUri}") },
                trackEntity.artist,
                trackEntity.loved,
                trackEntity.genre,
                trackEntity.duration,
                trackEntity.created,
                trackEntity.modified
            )
        }
    }

    suspend fun getTracksByLocation(
        db: AppDatabase,
        location: ContainerLocation
    ): List<TrackContainer> {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val result = playlistDao.getTracksByLocation(location).map { trackEntity ->
                TrackContainer(
                    trackEntity.trackId,
                    trackEntity.title,
                    trackEntity.path,
                    trackEntity.location,
                    0, // TODO (dp-18.12.21) :: retrieve correct order
//                    MetadataRetriever.byteToBitmap(trackEntity.image),
                    null,
                    trackEntity.imageByteArray,
                    trackEntity.imageBitmapUri?.let { Uri.parse("${App.applicationContext().cacheDir}${trackEntity.imageBitmapUri}") },
                    trackEntity.artist,
                    trackEntity.loved,
                    trackEntity.genre,
                    trackEntity.duration,
                    trackEntity.created,
                    trackEntity.modified
                )
            }

            result
        }
    }

    suspend fun addPlaylist(db: AppDatabase, playlist: Playlist) {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()
            val currentDate = Date()
            playlist.created = currentDate
            playlist.modified = currentDate

            playlistDao.addPlaylist(
                PlaylistEntity(
                    playlist.id,
                    playlist.name,
                    playlist.size,
                    playlist.image,
                    playlist.order,
                    playlist.favourite,
                    playlist.duration,
                    currentDate,
                    currentDate
                )
            )
        }
    }

    suspend fun updatePlaylist(db: AppDatabase, playlist: Playlist) {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            playlistDao.updatePlaylist(
                PlaylistEntity(
                    playlist.id,
                    playlist.name,
                    playlist.size,
                    playlist.image,
                    playlist.order,
                    playlist.favourite,
                    playlist.duration,
                    playlist.created,
                    Date()
                )
            )
        }
    }

    suspend fun updatePlaylistImage(db: AppDatabase, playlist: Playlist, imageUri: String) {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            playlistDao.updatePlaylistImage(playlist.id, imageUri)
        }
    }

    suspend fun playlistExists(db: AppDatabase, id: String): Boolean {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            playlistDao.playlistExists(id)
        }
    }

    suspend fun loveTrack(db: AppDatabase, track: TrackContainer): Boolean {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()
            val currentDate = Date()

            val imageId = track.id.toIntOrNull()?.let { track.id } ?: track.id.hashCode().toString()
            val imageUrl = track.imageByteArray?.let {
                App.getImageCache().saveToCacheAndGetUri(
                    MetadataRetriever.byteToBitmap(track.imageByteArray)!!,
                    imageId
                ).path
            }

            val trackEntity = TrackEntity(
                track.id,
                track.name,
                track.path,
                track.location,
                track.imageByteArray,
                imageUrl,
                track.artist,
                track.loved,
                track.genre,
                track.duration,
                track.created,
                currentDate
            )

            playlistDao.love(trackEntity, track.loved)

            true
        }
    }

    suspend fun addTracksToPlaylist(
        db: AppDatabase,
        playlistId: String,
        tracks: List<TrackContainer>
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val playlistDao = db.playlistDao()

            val trackEntities = tracks.map { track ->
                val imageUrl = track.imageByteArray?.let {
                    App.getImageCache().saveToCacheAndGetUri(
                        MetadataRetriever.byteToBitmap(track.imageByteArray)!!,
                        track.id
                    ).path
                }

                TrackEntity(
                    track.id,
                    track.name,
                    track.path,
                    track.location,
                    track.imageByteArray,
                    imageUrl,
                    track.artist,
                    track.loved,
                    track.genre,
                    track.duration,
                    track.created,
                    track.modified
                )
            }

            playlistDao.addTracksToPlaylist(playlistId, trackEntities)
        }
    }

    suspend fun deleteTracks(db: AppDatabase, trackIdAndPath: List<TrackContainer>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val playlistDao = db.playlistDao()

                val trackIds: List<String> = trackIdAndPath.map { it.id }
                val trackPaths: List<String> = trackIdAndPath.map { it.path }

                trackPaths.forEach { path ->
                    File(path).delete()
                }

                trackIds.forEach { trackId ->
                    File(ImageCache.getPath(trackId)).delete()
                }

                playlistDao.deleteTracks(trackIds)

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun removeTracksFromPlaylist(
        db: AppDatabase,
        trackIds: List<String>,
        playlistId: String
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val playlistDao = db.playlistDao()

                playlistDao.removeTracksFromPlaylist(trackIds, playlistId)

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun deletePlaylists(db: AppDatabase, playlists: List<Playlist>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val playlistDao = db.playlistDao()

                playlistDao.deletePlaylists(playlists.map { it.id })

                playlists.forEach { playlist ->
                    playlist.image?.let { image ->
                        File(ImageCache.getUri(image).path!!).delete()
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun deletePlaylistAndTracks(db: AppDatabase, playlist: Playlist): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val playlistDao = db.playlistDao()
                val tracksToDelete =
                    playlist.tracks.filter { it.location == ContainerLocation.LOCAL }
                val tracksDeletedSuccessfully = deleteTracks(db, tracksToDelete)

                if (tracksDeletedSuccessfully) {
                    playlistDao.deletePlaylists(listOf(playlist.id))

                    playlist.image?.let { image ->
                        File(ImageCache.getUri(image).path!!).delete()
                    }

                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}
