package com.delprks.wave.sections.adapters

import android.app.Activity
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.StyleSpan
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import android.view.SubMenu
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.*
import com.delprks.wave.sections.DownloadsFragment
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.*
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import kotlinx.coroutines.*
import wave.R
import wave.databinding.DownloadedListItemBinding
import java.io.File

class DownloadListViewRecyclerAdapter(
    private val tracks: List<TrackContainer>,
    private val parentActivity: Activity?,
    private val db: AppDatabase,
    private val context: DownloadsFragment
) : RecyclerView.Adapter<DownloadListViewRecyclerAdapter.ViewHolder>() {

    private var selectedPos = RecyclerView.NO_POSITION
    private val playerListener: Player.Listener

    init {
        playerListener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                super.onMediaItemTransition(mediaItem, reason)

                var positionInPlaylist = 0

                tracks.forEach { track ->
                    if (track.path == mediaItem?.mediaId) {
                        notifyItemChanged(selectedPos)
                        selectedPos = positionInPlaylist
                        notifyItemChanged(selectedPos)

                        val loveButton = parentActivity?.findViewById<ImageView>(R.id.main_player_love)

                        setLoveImage(track, loveButton)

                        loveButton?.setOnClickListener {
                            love(track)

                            setLoveImage(track, loveButton)
                        }

                        return@forEach
                    } else {
                        positionInPlaylist++
                    }
                }
            }
        }
    }

    private fun setLoveImage(track: TrackContainer, loveButton: ImageView?) {
        if (track.loved) {
            loveButton?.setImageResource(R.drawable.loved_track)
        } else {
            loveButton?.setImageResource(R.drawable.unloved_track)
        }
    }

    private fun love(track: TrackContainer) {
        track.loved = !track.loved

        CoroutineScope(Dispatchers.Main).launch {
            PlaylistService.loveTrack(db, track)
        }

        val message = if (track.loved) "Added ${track.name} to your Loved Tracks playlist"
        else "Removed ${track.name} from your Loved Tracks playlist"

        Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DownloadedListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]

        val itemTextSizeLimit =
            if (Display.isPortrait(parentActivity!!.resources))
                parentActivity.resources.getInteger(R.integer.list_item_text_size_limit_portrait)
            else
                parentActivity.resources.getInteger(R.integer.list_item_text_size_limit_landscape)

        holder.downloadTrackName.text = TextFormatter.shorten(track.name, itemTextSizeLimit)
        holder.downloadTrackArtist.text = track.artist ?: "Unknown artist"

        val trackDuration = DateUtils.formatElapsedTime(track.duration!!)

        holder.downloadedTrackLength.text = parentActivity.resources.getString(R.string.track_length_txt, trackDuration)

        if (track.imageByteArray != null) {
            holder.downloadedTrackImage.setImageBitmap(MetadataRetriever.byteToBitmap(track.imageByteArray))
        } else {
            holder.downloadedTrackImage.setImageResource(R.drawable.cover)
        }

//        if (track.loved) {
//            holder.downloadedLove.setImageResource(R.drawable.loved_track)
//        } else {
//            holder.downloadedLove.setImageResource(R.drawable.unloved_track)
//        }
//
//        holder.downloadedLove.setOnClickListener {
//            CoroutineScope(Dispatchers.Main).launch {
//                track.loved = !track.loved
//                PlaylistService.loveTrack(db, track)
//            }
//
//            notifyItemChanged(selectedPos)
//            selectedPos = holder.bindingAdapterPosition
//            notifyItemChanged(selectedPos)
//            selectedPos = RecyclerView.NO_POSITION
//        }

        holder.itemView.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.bindingAdapterPosition
            notifyItemChanged(selectedPos)
            selectedPos = RecyclerView.NO_POSITION

            context.playerService()?.loadSongs(parentActivity!!, tracks, "downloaded_section", playerListener, false)
            context.playerService()?.play(position)
        }

        holder.downloadedFileOptions.setOnClickListener {
            val popup = PopupMenu(parentActivity!!, holder.downloadedFileOptions)

            CoroutineScope(Dispatchers.Main).launch {
                val playlistSubMenu: SubMenu = popup.menu.addSubMenu(1, 1, 0, "Add to playlist")

                val playlists = PlaylistService.getAllPlaylistsWithoutTracks(db).filter { !ReservedPlaylists.isReserved(it.id) } as ArrayList<Playlist>

                val createPlaylistText = SpannableString(parentActivity.resources.getString(R.string.create_new_playlist_menu_item))
                createPlaylistText.setSpan(StyleSpan(Typeface.BOLD), 0, createPlaylistText.length, 0)

                playlistSubMenu.add(1, 0, 0, createPlaylistText).icon = ResourcesCompat.getDrawable(parentActivity.resources, R.drawable.add_playlist, null)

                if (playlists.isNotEmpty()) {
                    playlists.forEach { playlist -> playlistSubMenu.add(1, Integer.parseInt(playlist.id), 0, playlist.name) }
                }

                popup.inflate(R.menu.downloaded_menu)

                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.downloaded_remove -> {
                            context.deleteDownloadedTrack(track)

                            true
                        }
                        1 -> {
                            true
                        }
                        else -> {
                            // create new playlist selected
                            if (it.itemId == 0) {
                                val builder = PlaylistBuilder.buildPlaylistDialog(parentActivity, playlists, null, ::playlistActions, track, LibraryAddToPlaylistType.ADD_TO_PLAYLIST, position)

                                builder.show()
                            } else {
                                playlistActions(it.itemId, playlists, track, LibraryAddToPlaylistType.ADD_TO_PLAYLIST, position, null)
                            }

                            true
                        }
                    }
                }

                popup.show()
            }
        }
    }

    private fun playlistActions(playlistId: Int, playlists: ArrayList<Playlist>, track: Container, type: LibraryAddToPlaylistType, position: Int, currentPlaylist: Playlist?) {
        val playlist: Playlist = currentPlaylist ?: playlists.find { playlist -> playlist.id == playlistId.toString() }!!
        track.order = playlist.size + 1

        CoroutineScope(Dispatchers.Main).launch {
            val trackFromDB = PlaylistService.getTracksByIds(db, listOf(track.id))[0]

            PlaylistService.addTracksToPlaylist(
                db, playlist.id, listOf(
                    TrackContainer(
                        track.id,
                        trackFromDB.name,
                        track.path,
                        ContainerLocation.LOCAL,
                        playlist.tracks.size + 1,
                        trackFromDB.image,
                        trackFromDB.imageByteArray,
                        trackFromDB.imageBitmapUri,
                        trackFromDB.artist,
                        trackFromDB.loved,
                        trackFromDB.genre,
                        trackFromDB.duration,
                        trackFromDB.created,
                        trackFromDB.modified
                    )
                )
            )

            // use track image for the newly created playlist
            if (currentPlaylist != null) {
                val trackIdToUseForImage = trackFromDB.id

                currentPlaylist.let {
                    // TODO (dp-15.01.22) :: Avoid db call
                    val trackToUseForPlaylistImage = PlaylistService.getTrackById(db, trackIdToUseForImage)

                    trackToUseForPlaylistImage.imageBitmapUri?.let {
                        val trackImageFullUri = it.path!!
                        val playlistImage = "${playlist.id}_cover${ImageCache.FILE_EXTENSION}"
                        val playlistImageUri = ImageCache.getUri(playlistImage)

                        // create copy of image to be used for playlist
                        File(trackImageFullUri).copyTo(
                            File(playlistImageUri.path!!),
                            true
                        )

                        playlist.image = playlistImage
                        currentPlaylist.image = playlistImage

                        PlaylistService.updatePlaylistImage(db, playlist, playlistImage)
                    }
                }
            }

            Toast.makeText(parentActivity, "Added ${track.name} to ${playlist.name} playlist", Toast.LENGTH_SHORT).show()
        }

    }

    override fun getItemCount(): Int = tracks.size

    inner class ViewHolder(binding: DownloadedListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val downloadTrackName: TextView = binding.downloadedTrackName
        val downloadTrackArtist: TextView = binding.downloadedArtistName
        val downloadedFileOptions: TextView = binding.downloadedFileOptions
        val downloadedTrackImage = binding.downloadedTrackImage
        val downloadedTrackLength = binding.downloadedTrackLength
//        val downloadedLove = binding.downloadedLove

        override fun toString(): String {
            return super.toString() + " '" + downloadTrackName.text + "'"
        }
    }

}
