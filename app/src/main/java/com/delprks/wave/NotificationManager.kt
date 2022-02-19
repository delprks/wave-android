package com.delprks.wave

import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.lifecycle.MutableLiveData
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import kotlinx.coroutines.*
import wave.R

const val NOW_PLAYING_CHANNEL_ID = "com.delprks.wave.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb405

@RequiresApi(Build.VERSION_CODES.N)
class NotificationManager(
    private val context: Context,
    sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {

    private var player: Player? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: PlayerNotificationManager
    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)

//            .apply {
//            registerCallback(MediaControllerCallback())
//        }

        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.notification_channel)
            .setChannelDescriptionResourceId(R.string.notification_channel_description)
            .setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            .setNotificationListener(notificationListener)
            .setChannelImportance(IMPORTANCE_DEFAULT)
            .build()

        notificationManager.setMediaSessionToken(sessionToken)
        notificationManager.setSmallIcon(R.drawable.ic_notification)

        // Don't display the rewind or fast-forward buttons.
        notificationManager.setUseFastForwardAction(false)
        notificationManager.setUseRewindAction(false)
        notificationManager.setUseNextActionInCompactView(true)
        notificationManager.setUsePreviousActionInCompactView(true)
        notificationManager.setPriority(PRIORITY_MAX)
        notificationManager.setColorized(true)
    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun setPlayer(player: Player?) {
        notificationManager.setPlayer(player)
    }

    @Suppress("PropertyName")
    val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
        .build()

    //
    @Suppress("PropertyName")
    val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
        .build()

    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }

    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }
//
//        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
//            // When ExoPlayer stops we will receive a callback with "empty" metadata. This is a
//            // metadata object which has been instantiated with default values. The default value
//            // for media ID is null so we assume that if this value is null we are not playing
//            // anything.
//            nowPlaying.postValue(
//                if (metadata?.id == null) {
//                    NOTHING_PLAYING
//                } else {
//                    metadata
////                }
//            )
//        }
//        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)
        }

        override fun onSessionDestroyed() {
            //mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }


    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {
        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri

            return if (currentIconUri != iconUri || currentBitmap == null) {
                // Cache the bitmap for the current song so that successive calls to
                // `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                currentIconUri = iconUri

                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }

                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(uri.path)
            }
        }

//        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
//            return withContext(Dispatchers.IO) {
//                // Block on downloading artwork.
//                Glide.with(context)
//                    .applyDefaultRequestOptions(glideOptions)
//                    .asBitmap()
//                    .load(uri)
//                    .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
//                    .get()
//            }
//        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
    .fallback(R.drawable.default_art)
    .diskCacheStrategy(DiskCacheStrategy.DATA)

private const val MODE_READ_ONLY = "r"
