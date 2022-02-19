package com.delprks.wave.dao

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RemoteSettingsEntity(
    @PrimaryKey var id: String,
    var type: RemoteSourceType,
    var name: String,
    var host: String,
    var mediaPath: String
)
