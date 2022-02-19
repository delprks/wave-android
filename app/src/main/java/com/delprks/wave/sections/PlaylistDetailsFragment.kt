package com.delprks.wave.sections

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.delprks.wave.*
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.Playlist
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.sections.adapters.PlayerListViewRecyclerAdapter
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.ImageCache
import com.delprks.wave.util.ReservedPlaylists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.material.bottomsheet.BottomSheetBehavior

import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import wave.R

class PlaylistDetailsFragment : BottomSheetDialogFragment() {

    private val db: AppDatabase = App.getDB()
    private lateinit var playerListViewAdapter: PlayerListViewRecyclerAdapter
    private lateinit var playlistListView: RecyclerView
    private lateinit var context: Activity
    private lateinit var playlist: Playlist

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        playlist = App.getCurrentPlaylist()
        context = requireActivity()

        val view = inflater.inflate(R.layout.playlist_details, container, false)

        view.findViewById<ImageView>(R.id.playlist_activity_close_button).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            // activity.onBackPressed() alt
        }

        val bottomSheet = view.findViewById<NestedScrollView>(R.id.playlist_details_layout)
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = STATE_EXPANDED

        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(view: View, i: Int) {
                when (i) {
                    BottomSheetBehavior.STATE_HIDDEN -> dismiss() //if you want the modal to be dismissed when user drags the bottomsheet down
                    STATE_EXPANDED -> {
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        dismiss()
                    }
                }
            }

            override fun onSlide(view: View, v: Float) {
                activity?.findViewById<NestedScrollView>(R.id.playlist_details_layout)?.alpha =
                    1 + v
            }
        })

        playlistListView = view.findViewById(R.id.player_activity_track_list)

        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    // move item in `fromPos` to `toPos` in adapter.
                    return true // true if moved, false otherwise
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.absoluteAdapterPosition

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            removePlaylistTrack(playlist.tracks[position])
                        }
                    }
                }
            })

        itemTouchHelper.attachToRecyclerView(playlistListView)

        context.window.statusBarColor =
            resources.getColor(R.color.playlist_activity_start_background)
        context.actionBar?.hide()

        val playlistImageView = view.findViewById<ImageView>(R.id.playlist_activity_image)

        when (playlist.id) {
            ReservedPlaylists.DOWNLOADS_PLAYLIST_ID -> {
                playlistImageView.setImageResource(R.drawable.downloads_playlist_cover)
            }

            ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID -> {
                playlistImageView.setImageResource(R.drawable.loved_tracks_playlist_cover)
            }

            else -> playlist.image?.let {
                playlistImageView.setImageURI(ImageCache.getUri(playlist.image!!))
            } ?: playlistImageView.setImageResource(R.drawable.cover)
        }

        view.findViewById<TextView>(R.id.playlist_activity_playlist_title).text = playlist.name
        view.findViewById<TextView>(R.id.playlist_activity_playlist_tracks_count_title).text =
            resources.getString(
                R.string.playlist_activity_track_count_text,
                playlist.size
            )

        CoroutineScope(Dispatchers.Main).launch {
            val playlistWithTracks = PlaylistService.getPlaylistPopulatedWithTracks(db, playlist.id)
            playlist.tracks = playlistWithTracks.tracks

            playerListViewAdapter = PlayerListViewRecyclerAdapter(playlist.tracks, context, -1, db, playlist.id, App.getPlaylistFragment())

            playlistListView.adapter = playerListViewAdapter
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun removePlaylistTrack(track: TrackContainer) {
        CoroutineScope(Dispatchers.Main).launch {
            val removedSuccessfully = when (playlist.id) {
                ReservedPlaylists.DOWNLOADS_PLAYLIST_ID -> PlaylistService.deleteTracks(
                    db,
                    listOf(track)
                )
                ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID -> {
                    track.loved = !track.loved
                    PlaylistService.loveTrack(db, track)
                }
                else -> PlaylistService.removeTracksFromPlaylist(
                    db,
                    listOf(track.id),
                    playlist.id
                )
            }

            if (removedSuccessfully) {
                playerListViewAdapter.refreshPlaylistsWithTrackRemoved(track)
                Toast.makeText(
                    activity,
                    "Removed ${track.name} from ${playlist.name} successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    activity,
                    "Failed to remove ${track.name} from ${playlist.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        const val PLAYLIST = "playlist"
        const val PLAYLIST_FRAGMENT = "playlist_fragment"
    }
}
