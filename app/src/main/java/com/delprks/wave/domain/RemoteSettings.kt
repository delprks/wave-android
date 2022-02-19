package com.delprks.wave.domain

import com.delprks.wave.dao.RemoteSourceType

data class RemoteSettings(
    var id: String,
    var type: RemoteSourceType,
    var name: String,
    var host: String,
    var mediaPath: String
)
