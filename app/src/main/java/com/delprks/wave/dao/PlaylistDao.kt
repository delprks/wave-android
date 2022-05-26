package com.delprks.wave.dao

import androidx.room.*
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.util.ReservedPlaylists
import java.util.*

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlistentity")
    fun getAll(): List<PlaylistWithTracksEntity>

    @Query("SELECT * FROM playlistentity ORDER BY `order` ASC")
    fun getAllPlaylistsWithoutTracks(): List<PlaylistEntity>

    @Query("SELECT * FROM playlistentity WHERE playlist_id = :id")
    fun getPlaylistWithoutTracks(id: String): PlaylistEntity

    @Query("SELECT * FROM playlistentity WHERE playlist_id IN (:ids)")
    fun getPlaylistsWithoutTracks(ids: List<String>): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertAll(vararg playlist: PlaylistEntity)

    @Query("SELECT COUNT(*) FROM playlistentity")
    fun countPlaylists(): Int

    @Update
    fun updatePlaylist(vararg playlist: PlaylistEntity)

    @Query("UPDATE playlistentity SET imageUri=:imageUri WHERE playlist_id=:playlistId")
    fun updatePlaylistImage(playlistId: String, imageUri: String)

    @Update
    fun updatePlaylists(playlist: List<PlaylistEntity>)

    @Query("SELECT * FROM latesttrackentity WHERE id=:id")
    fun getLatestTrack(id: String): LatestTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateLatestTrack(latestTrackEntity: LatestTrackEntity)

    @Transaction
    fun addPlaylist(playlist: PlaylistEntity) {
        playlist.order = countPlaylists()

        insertAll(playlist)
    }

    @Transaction
    fun love(track: TrackEntity, loved: Boolean) {
        if (loved) {
            addTracksToPlaylist(ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID, listOf(track))
        } else {
            removeTracksFromPlaylist(
                listOf(track.trackId),
                ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID
            )
        }

        loveTrack(track.trackId, loved)
    }

    @Query("UPDATE trackentity SET loved=:loved WHERE track_id=:trackId")
    fun loveTrack(trackId: String, loved: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addTracksAndReplace(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addTracksAndSkip(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addTrackPlaylistRelations(playlistTrackRelation: List<PlaylistTrackEntity>)

    @Query("SELECT * FROM trackentity WHERE track_id IN (:ids) ORDER BY modified")
    fun getTracksById(ids: List<String>): List<TrackEntity>

    @Query("SELECT * FROM trackentity WHERE track_id = :id")
    fun getTrackById(id: String): TrackEntity

    @Query("SELECT * FROM trackentity WHERE location = :location ORDER BY modified")
    fun getTracksByLocation(location: ContainerLocation): List<TrackEntity>

    @Query("SELECT * FROM playlistentity WHERE playlist_id = :id")
    fun getPlaylistPopulatedWithTracks(id: String): PlaylistWithTracksEntity

    @Transaction
    fun addTracksToPlaylist(playlistId: String, tracks: List<TrackEntity>): Boolean {
        val playlistWithoutTracks = getPlaylistWithoutTracks(playlistId)

        val localTracks = tracks.filter { it.location == ContainerLocation.LOCAL }
        val remoteTracks = tracks.filter { it.location == ContainerLocation.REMOTE }

        addTracksAndReplace(localTracks)
        addTracksAndSkip(remoteTracks)

        val trackPlaylistRelation = tracks.map { trackEntity -> PlaylistTrackEntity(playlistId, trackEntity.trackId, 0) }
        addTrackPlaylistRelations(trackPlaylistRelation)

        val tracksForPlaylist = getTrackPlaylistRelationByPlaylistId(playlistId)

        playlistWithoutTracks.size = tracksForPlaylist.size
        playlistWithoutTracks.duration += tracks.sumOf { it.duration ?: run { 0L } }
        playlistWithoutTracks.modified = Date()

        updatePlaylist(playlistWithoutTracks)

        return true
    }

    @Transaction
    fun deleteTracks(trackIds: List<String>) {
        //TODO::need to figure out how many tracks exist in each playlist, rather than deleting same number from all playlists
        val trackPlaylistRelation = getTrackPlaylistRelationByTrackIds(trackIds)
        val tracks = getTracksById(trackIds)
        val affectedPlaylists = getPlaylistsWithoutTracks(trackPlaylistRelation.map { it.playlistId }).distinct()
        val affectedPlaylistsMap: Map<String, List<PlaylistTrackEntity>> = trackPlaylistRelation.groupBy { it.playlistId }.mapValues { it.value.distinct() }

        val currentDate = Date()

        affectedPlaylists.forEach { playlistEntity ->
            val tracksRemovedFromThisPlaylist: List<PlaylistTrackEntity> = affectedPlaylistsMap[playlistEntity.playlistId]!!

            val removedTracksSize = tracksRemovedFromThisPlaylist.size
            val removedTracksDuration = tracks.filter { tracksRemovedFromThisPlaylist.map { entity -> entity.trackId }.contains(it.trackId) }.sumOf { it.duration ?: run { 0L } }
            playlistEntity.size -= removedTracksSize
            playlistEntity.modified = currentDate
            playlistEntity.duration -= removedTracksDuration
        }

        removeTracks(trackIds)

        updatePlaylists(affectedPlaylists)

        removeTrackPlaylistRelationByTrackIds(trackIds)
    }

    @Transaction
    fun deletePlaylists(playlistIds: List<String>) {
        removePlaylists(playlistIds)
        removeTrackPlaylistRelationByPlaylistIds(playlistIds)
    }

    @Query("SELECT * FROM playlisttrackentity WHERE track_id IN (:ids)")
    fun getTrackPlaylistRelationByTrackIds(ids: List<String>): List<PlaylistTrackEntity>

    @Query("SELECT * FROM playlisttrackentity WHERE playlist_id = :id")
    fun getTrackPlaylistRelationByPlaylistId(id: String): List<PlaylistTrackEntity>

    @Query("DELETE FROM trackentity WHERE track_id IN (:ids)")
    fun removeTracks(ids: List<String>)

    @Query("DELETE FROM playlisttrackentity WHERE track_id IN (:ids)")
    fun removeTrackPlaylistRelationByTrackIds(ids: List<String>)

    @Query("DELETE FROM playlisttrackentity WHERE track_id IN (:trackIds) AND playlist_id = :playlistId")
    fun removeTrackPlaylistRelationByTrackIdAndPlaylistId(
        trackIds: List<String>,
        playlistId: String
    )

    @Transaction
    fun removeTracksFromPlaylist(trackIds: List<String>, playlistId: String) {
        val playlistEntity = getPlaylistWithoutTracks(playlistId)

        val tracks = getTracksById(trackIds)
        val removedTracksSize = tracks.size

        playlistEntity.size -= removedTracksSize
        playlistEntity.modified = Date()
        playlistEntity.duration -= tracks.sumOf { it.duration ?: run { 0L } }

        removeTrackPlaylistRelationByTrackIdAndPlaylistId(trackIds, playlistId)
        updatePlaylist(playlistEntity)
    }

    @Query("DELETE FROM playlisttrackentity WHERE playlist_id IN (:ids)")
    fun removeTrackPlaylistRelationByPlaylistIds(ids: List<String>)

    @Query("DELETE FROM playlistentity WHERE playlist_id IN (:ids)")
    fun removePlaylists(ids: List<String>)

    @Query("SELECT EXISTS(SELECT * FROM playlistentity WHERE playlist_id = :id)")
    fun playlistExists(id: String): Boolean

    @Query("SELECT EXISTS(SELECT * FROM trackentity WHERE track_id = :id)")
    fun trackExists(id: String): Boolean
}
