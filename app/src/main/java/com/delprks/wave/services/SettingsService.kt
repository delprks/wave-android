package com.delprks.wave.services

import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.dao.RemoteSettingsEntity
import com.delprks.wave.domain.RemoteSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SettingsService {
    const val WEB_DAV_ID = "webdav"

    suspend fun getWebDavRemoteSettings(db: AppDatabase): RemoteSettings? {
        return withContext(Dispatchers.IO) {
            val settingsDao = db.settingsDao()

            settingsDao.getRemoteSettingsById("webdav")?.let { settings ->
                RemoteSettings(
                    settings.id,
                    settings.type,
                    settings.name,
                    settings.host,
                    settings.mediaPath
                )
            }
        }
    }

    suspend fun addWebDavRemoteSettings(db: AppDatabase, remoteSettings: RemoteSettings) {
        return withContext(Dispatchers.IO) {
            val settingsDao = db.settingsDao()

            val remoteSettingsEntity = RemoteSettingsEntity(
                remoteSettings.id,
                remoteSettings.type,
                remoteSettings.name,
                remoteSettings.host,
                remoteSettings.mediaPath
            )

            settingsDao.addRemoteSettings(remoteSettingsEntity)
        }
    }
}
