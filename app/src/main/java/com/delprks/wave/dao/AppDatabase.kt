package com.delprks.wave.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.delprks.wave.util.DateTimeConverter

@Database(entities = [PlaylistEntity::class, PlaylistTrackEntity::class, TrackEntity::class, TrackStatusEntity::class, RemoteSettingsEntity::class], version = 8)
@TypeConverters(DateTimeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun settingsDao(): SettingsDao
}
