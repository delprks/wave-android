package com.delprks.wave

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.Playlist
import com.delprks.wave.sections.PlaylistFragment
import com.delprks.wave.util.ImageCache
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class App : Application(){

    init {
        instance = this
    }

    override fun onCreate(){
        super.onCreate()

        setupSSL()
        createNotificationChannels()
    }

    private fun setupSSL() {
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory)
        }
    }

    //Check if the Android version is greater than 8. (Android Oreo)
    private fun createNotificationChannels(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val progressNotificationChannel = NotificationChannel(
                PROGRESS_NOTIFICATION_ID,
                PROGRESS_NOTIFICATION_NAME,
                IMPORTANCE_HIGH
            )
            progressNotificationChannel.description = PROGRESS_NOTIFICATION_DESCRIPTION

            val manager: NotificationManager? = getSystemService(
                NotificationManager::class.java
            )

            manager!!.createNotificationChannel(progressNotificationChannel)
        }
    }

    companion object {
        const val PROGRESS_NOTIFICATION_ID = "com.delprks.wave.PROGRESS_NOTIFICATION"
        const val PROGRESS_NOTIFICATION_NAME = "Progress Notification"
        const val PROGRESS_NOTIFICATION_DESCRIPTION = "Progress Notification Channel"

        private var db: AppDatabase? = null
        private var instance: App? = null
        private var imageCache: ImageCache? = null
        private var playlistFragment: PlaylistFragment? = null
        private var currentPlaylist: Playlist? = null

        fun applicationContext() : Context {
            return instance!!.applicationContext
        }

        fun getDB(): AppDatabase {
            if (db == null) {
                db = Room.databaseBuilder(
                    applicationContext(),
                    AppDatabase::class.java, "wave-db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
            }

            return db!!
        }

        fun getImageCache(): ImageCache {
            if (imageCache == null) {
                imageCache = ImageCache()
            }

            return imageCache!!
        }

        fun getPlaylistFragment(): PlaylistFragment {
            if (playlistFragment == null) {
                playlistFragment = PlaylistFragment()
            }

            return playlistFragment!!
        }

        fun getCurrentPlaylist(): Playlist {
            return currentPlaylist!!
        }

        fun setCurrentPlaylist(playlist: Playlist) {
            currentPlaylist = playlist
        }
    }
}
