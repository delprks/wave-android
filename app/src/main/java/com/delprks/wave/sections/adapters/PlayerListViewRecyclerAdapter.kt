package com.delprks.wave.sections.adapters

import android.app.Activity
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.res.ResourcesCompat
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.sections.PlaylistFragment
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.ReservedPlaylists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.IllegalArgumentException
import com.delprks.wave.util.SwipeDetector

import com.delprks.wave.util.SwipeDetector.SwipeTypeEnum
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerView
import wave.R
import wave.databinding.PlayerListItemBinding

@RequiresApi(Build.VERSION_CODES.O)
class PlayerListViewRecyclerAdapter(
    private val tracks: List<TrackContainer>,
    private val parentActivity: Activity?,
    private var selectedPosition: Int,
    private val db: AppDatabase,
    private val playlistId: String?,
    private val playlistFragment: PlaylistFragment?
) : RecyclerView.Adapter<PlayerListViewRecyclerAdapter.ViewHolder>() {
    private val trackCountTxt: TextView? = parentActivity?.findViewById(R.id.playlist_activity_playlist_tracks_count_title)
    private val playerListener: Player.Listener
    private var selectedPos = selectedPosition

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PlayerListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val track = tracks[position]

        holder.trackName.text = track.name
        holder.trackArtist.text = track.artist

        if (track.location == ContainerLocation.REMOTE) {
            holder.itemView.findViewById<RelativeLayout>(R.id.player_list_item).background = ResourcesCompat.getDrawable(parentActivity!!.resources, R.drawable.list_item_background_remote, null)
        }

//        if (track.imageBitmapUri != null) {
//            holder.trackImage.setImageURI(track.imageBitmapUri)
//        }

        holder.menuOptions.setOnClickListener {
            val popup = PopupMenu(parentActivity!!, holder.menuOptions)

            popup.inflate(R.menu.player_track_menu)

            if (track.location == ContainerLocation.LOCAL) {
                popup.menu.removeItem(R.id.player_download_track)
            } else {
                popup.menu.removeItem(R.id.player_delete_track)
            }

            if (playlistId != null && ReservedPlaylists.isReserved(playlistId)) {
                popup.menu.removeItem(R.id.player_remove_track_from_playlist)
            }

            if (track.loved) {
                popup.menu.removeItem(R.id.player_love_track)
            } else {
                popup.menu.removeItem(R.id.player_unlove_track)
            }

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.player_love_track, R.id.player_unlove_track -> {
                        love(track)

                        true
                    }
                    R.id.player_delete_track -> {
                        deleteDownloadedTrack(track)

                        true
                    }
                    R.id.player_remove_track_from_playlist -> {
                        if (playlistId != null) {
                            removeTrackFromPlaylist(track, playlistId)
                        }

                        true
                    }
                    else -> throw IllegalArgumentException("Invalid menu item selected")
                }
            }

            popup.show()
        }

        holder.itemView.setOnClickListener {
            playlistFragment?.playerService()?.loadSongs(parentActivity!!, tracks, playlistId, playerListener, false)
            playlistFragment?.playerService()?.play(position)

            notifyItemChanged(selectedPos)
            selectedPos = holder.bindingAdapterPosition
            notifyItemChanged(selectedPos)
        }

        val mediaPlayer = parentActivity!!.findViewById<PlayerView>(R.id.main_media_player)

        SwipeDetector(mediaPlayer).setOnSwipeListener { _, swipeType ->
            if (swipeType == SwipeTypeEnum.LEFT_TO_RIGHT) {
                selectedPosition = if (selectedPos == 0) 0 else selectedPos - 1

                playlistFragment?.playerService()?.loadSongs(parentActivity, tracks, playlistId, playerListener, false)
                playlistFragment?.playerService()?.play(selectedPosition)
            } else if (swipeType == SwipeTypeEnum.RIGHT_TO_LEFT) {
                selectedPosition = if (selectedPos == tracks.size - 1) 0 else selectedPos + 1

                playlistFragment?.playerService()?.loadSongs(parentActivity, tracks, playlistId, playerListener, false)
                playlistFragment?.playerService()?.play(selectedPosition)
            }

            notifyItemChanged(selectedPos)
            selectedPos = selectedPosition
            notifyItemChanged(selectedPos)
        }

        parentActivity.findViewById<Button>(R.id.player_activity_play_btn).setOnClickListener {
            selectedPosition = 0

            notifyItemChanged(selectedPos)
            selectedPos = selectedPosition
            notifyItemChanged(selectedPos)

            playlistFragment?.playerService()?.loadSongs(parentActivity, tracks, playlistId, playerListener, false)
            playlistFragment?.playerService()?.play(0)
        }

        parentActivity.findViewById<Button>(R.id.player_activity_shuffle_button).setOnClickListener {
            selectedPosition = 0

            notifyItemChanged(selectedPos)
            selectedPos = selectedPosition
            notifyItemChanged(selectedPos)

            playlistFragment?.playerService()?.loadSongs(parentActivity, tracks, playlistId, playerListener, false)
            playlistFragment?.playerService()?.play(0, true)
        }

        if (selectedPos >= 0) {
            holder.itemView.isSelected = selectedPos == position
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

        if (playlistId == ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID) {
            refreshPlaylistsWithTrackRemoved(track)
        }

        Toast.makeText(parentActivity, message, Toast.LENGTH_SHORT).show()
    }

    fun refreshPlaylistsWithTrackRemoved(track: TrackContainer) {
        val position = tracks.indexOf(track)

        (tracks as ArrayList<TrackContainer>).removeAt(position)

        playlistFragment?.playerService()?.loadSongs(parentActivity!!, tracks, playlistId, playerListener, true)

        notifyItemRemoved(position)

        trackCountTxt?.text = parentActivity?.resources?.getString(
            R.string.playlist_activity_track_count_text,
            tracks.size
        )
    }

    private fun deleteDownloadedTrack(track: TrackContainer) {
        CoroutineScope(Dispatchers.Main).launch {
            val deletedSuccessfully = PlaylistService.deleteTracks(db, listOf(track))

            if (deletedSuccessfully) {
                refreshPlaylistsWithTrackRemoved(track)

                Toast.makeText(
                    parentActivity,
                    "Deleted ${track.name} successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(parentActivity, "Failed to delete ${track.name}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun removeTrackFromPlaylist(track: TrackContainer, playlistId: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val removedSuccessfully =
                PlaylistService.removeTracksFromPlaylist(db, listOf(track.id), playlistId)

            if (removedSuccessfully) {
                refreshPlaylistsWithTrackRemoved(track)

                Toast.makeText(
                    parentActivity,
                    "Removed ${track.name} successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(parentActivity, "Failed to remove ${track.name}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun getItemCount(): Int = tracks.size

    inner class ViewHolder(binding: PlayerListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val trackName: TextView = binding.playerSongName
        val trackImage: ImageView = binding.playerSongImage
        val trackArtist: TextView = binding.playerArtistName
        val menuOptions: TextView = binding.playerTrackOptions

        override fun toString(): String {
            return super.toString() + " '" + trackName.text + "'"
        }
    }

}
