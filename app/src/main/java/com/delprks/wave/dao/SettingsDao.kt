package com.delprks.wave.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addRemoteSettings(remoteSettings: RemoteSettingsEntity)

    @Query("SELECT * FROM RemoteSettingsEntity WHERE id = :id")
    fun getRemoteSettingsById(id: String): RemoteSettingsEntity?
}
