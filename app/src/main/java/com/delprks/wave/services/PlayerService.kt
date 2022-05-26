package com.delprks.wave.services

import android.app.*
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.support.v4.media.session.MediaSessionCompat
import android.widget.TextView
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.Player
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.HttpDataSource
import android.widget.ImageView
import androidx.core.content.res.ResourcesCompat
import com.delprks.wave.App
import com.delprks.wave.NotificationManager
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.domain.LatestTrack
import com.delprks.wave.security.SettingsManager
import com.google.android.exoplayer2.C.WAKE_MODE_NETWORK
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.MediaMetadata.PICTURE_TYPE_FRONT_COVER
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wave.R

class PlayerService : Service() {
    private var notificationManager: NotificationManager? = null
    private lateinit var mediaSessionCompat: MediaSessionCompat
    private lateinit var tracks: List<TrackContainer>
    private lateinit var tracksMap: HashMap<String, TrackContainer>
    private lateinit var activity: Activity

    private val context = App.applicationContext()

    private var player: ExoPlayer
    private var mediaSessionConnector: MediaSessionConnector? = null
    private var playlistId: String? = null
    private var trackSelector: DefaultTrackSelector
    private val mediaSourceFactory: DefaultMediaSourceFactory
    private var playerCollapsed = true

    // Binder given to clients
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    init {
        val httpDataSourceFactory: HttpDataSource.Factory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(SettingsManager.getAuthMap(context))

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        trackSelector = DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setMaxVideoSizeSd())
        }

        mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        player = ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun loadSongs(activity: Activity, tracks: List<TrackContainer>, playlistId: String?, listener: Player.Listener?, reload: Boolean = false) {
        this.tracks = tracks
        this.activity = activity

        if (playlistId != this.playlistId || reload) {
            player.stop()
            player.release()

            player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setTrackSelector(trackSelector)
                .build()

            player.setWakeMode(WAKE_MODE_NETWORK)

            tracksMap = HashMap()

            player.also { exoPlayer ->
                activity.findViewById<PlayerView>(R.id.main_media_player).player = exoPlayer
                activity.findViewById<PlayerView>(R.id.mini_media_player).player = exoPlayer

                tracks.forEach { track ->
                    run {
                        val mediaItem = MediaItem.Builder()
                            .setMediaId(track.id)
                            .setUri(track.path)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle(track.name)
                                    .setArtist(track.artist)
                                    .setArtworkData(track.imageByteArray, PICTURE_TYPE_FRONT_COVER)
                                    .build()
                            )
                            .build()

                        exoPlayer.addMediaItem(
                            mediaItem
                        )

                        exoPlayer.playWhenReady = true
                    }
                }
            }

            val sessionActivityPendingIntent = activity.packageManager?.getLaunchIntentForPackage(activity.packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(context, 0, sessionIntent, FLAG_MUTABLE)
            }

            // Create a new MediaSession.
            mediaSessionCompat = MediaSessionCompat(context, "MusicService")
                .apply {
                    setSessionActivity(sessionActivityPendingIntent)
                    isActive = true
                }

            mediaSessionConnector = MediaSessionConnector(mediaSessionCompat)

            mediaSessionConnector!!.setQueueNavigator(object : TimelineQueueNavigator(mediaSessionCompat) {
                override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
                    val currentTrack = tracks[windowIndex]
                    tracksMap[currentTrack.path] = currentTrack
                    val artist = currentTrack.artist ?: "Unknown artist"

                    mediaSessionCompat.setMetadata(createMediaMetadataCompat(currentTrack.name, artist, currentTrack.imageBitmapUri))

                    return MediaDescriptionCompat.Builder()
                        .setMediaId(currentTrack.id)
                        .setMediaUri(Uri.parse(currentTrack.path))
                        .setTitle(currentTrack.name)
                        .setSubtitle(artist)
                        .setIconUri(currentTrack.imageBitmapUri)
                        .build()
                }
            })

            mediaSessionConnector!!.setPlayer(player)

            player.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    super.onMediaItemTransition(mediaItem, reason)

                    activity.findViewById<TextView>(R.id.player_track_title).text = activity.resources.getString(R.string.player_track_title_txt, mediaItem?.mediaMetadata?.title)
                    activity.findViewById<TextView>(R.id.player_track_artist).text = activity.resources.getString(R.string.player_track_artist_txt, mediaItem?.mediaMetadata?.artist)
                    activity.findViewById<TextView>(R.id.mini_player_track_title).text = mediaItem?.mediaMetadata?.title
                    activity.findViewById<TextView>(R.id.mini_player_track_artist).text = mediaItem?.mediaMetadata?.artist ?: "Unknown artist"
                    activity.findViewById<ImageView>(R.id.mini_player_track_image).setImageURI(tracksMap[mediaItem?.mediaId]?.imageBitmapUri)

                    CoroutineScope(Dispatchers.Main).launch {
                        val trackStatus = LatestTrack(
                            trackId = mediaItem!!.mediaId,
                            trackProgress = 0,
                            shuffled = player.shuffleModeEnabled,
                            playlistId = playlistId
                        )
                        PlaylistService.updateTrackStatus(App.getDB(), trackStatus)

                        Log.d("player", "updated latest track: $trackStatus")
                    }
                }
            })

            notificationManager = NotificationManager(context, mediaSessionCompat.sessionToken, PlayerNotificationListener(player, this))
            notificationManager?.setPlayer(player)
        }

        listener?.let { l -> player.addListener(l) }

        this.playlistId = playlistId
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun play(position: Int, shuffled: Boolean = false, paused: Boolean = false, initial: Boolean = false): ExoPlayer {
        val track = tracks[position]

        if (playerCollapsed) {
            val playerView = activity.findViewById<SlidingUpPanelLayout>(R.id.slidingUpPlayer)

            playerView.panelHeight = activity.resources.getDimensionPixelSize(R.dimen.mini_player_height)

            playerCollapsed = false
        }

        if (!initial) {
            CoroutineScope(Dispatchers.Main).launch {
                val trackStatus = LatestTrack(
                    trackId = track.id,
                    trackProgress = 0,
                    shuffled = shuffled,
                    playlistId = playlistId
                )
                PlaylistService.updateTrackStatus(App.getDB(), trackStatus)

                Log.d("player", "updated latest track: $trackStatus")
            }
        }

        player.shuffleModeEnabled = shuffled
        player.seekTo(position, 0)

        val trackTitle: String = track.name
        val trackArtist: String = track.artist ?: "Unknown artist"

        activity.findViewById<TextView>(R.id.player_track_title).text = activity.resources.getString(R.string.player_track_title_txt, trackTitle)
        activity.findViewById<TextView>(R.id.player_track_artist).text = activity.resources.getString(R.string.player_track_artist_txt, trackArtist)
        activity.findViewById<TextView>(R.id.mini_player_track_title).text = trackTitle
        activity.findViewById<TextView>(R.id.mini_player_track_artist).text = trackArtist

        track.imageBitmapUri?.let { uri ->
            activity.findViewById<ImageView>(R.id.mini_player_track_image).setImageURI(uri)
        } ?: activity.findViewById<ImageView>(R.id.mini_player_track_image).setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.cover, null))

        player.prepare()

        if (paused) {
            player.pause()
        } else {
            player.play()
        }

        return player
    }

    private fun createMediaMetadataCompat(title: String?, artist: String?, imageUri: Uri?): MediaMetadataCompat {
        return MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, imageUri?.path)
            .build()
    }

    /**
     * Listen for notification events.
     */
    private class PlayerNotificationListener(val player: Player, val playerService: PlayerService) :
        PlayerNotificationManager.NotificationListener {

        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            super.onNotificationPosted(notificationId, notification, ongoing)
            if (!ongoing) {
                playerService.stopForeground(false)
            } else {
                playerService.startForeground(notificationId, notification)
            }

//            if (ongoing) {
//                ContextCompat.startForegroundService(
//                    context,
//                    Intent(context, this::class.java)
//                )
//
//                playerService.startForeground(notificationId, notification)
//            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            player.stop()
            player.release()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("player", "onstart called")
//        notificationManager.deleteNotificationChannel("channel_id");
        return START_STICKY
    }

    // detach player
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        stopSelf()
        stopForeground(true)

        Log.d("player", "destroying")
        notificationManager?.hideNotification()
        player.release()
        super.onDestroy()
    }

    //removing service when user swipe out our app
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("player", "quitting task")
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
}
