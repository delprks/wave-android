package com.delprks.wave.sections

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.delprks.wave.*
import com.delprks.wave.sections.adapters.PlaylistsListViewRecyclerAdapter
import java.util.*
import android.widget.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.domain.Playlist
import com.delprks.wave.services.PlayerService
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.PlaylistBuilder
import com.delprks.wave.util.ReservedPlaylists
import kotlinx.coroutines.*
import wave.R
import kotlin.collections.ArrayList

class PlaylistFragment : Fragment() {

    lateinit var itemTouchHelper: ItemTouchHelper
    private val viewModel: SharedTitle by activityViewModels()
    private lateinit var playlistsAdapter: PlaylistsListViewRecyclerAdapter
    private lateinit var playlists: ArrayList<Playlist>
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var progressNotification: NotificationCompat.Builder
    private val maxProgress = 100

    fun playerService(): PlayerService? {
        return (requireActivity() as TabbedHomeActivity).playerService
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.playlists_list, container)
        val recyclerLayout = view.findViewById<RecyclerView>(R.id.playlists_list)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshPlaylistsLayout)
        val addPlaylistButton = view.findViewById<Button>(R.id.add_playlist_btn)
        val context = this

        notificationManager = NotificationManagerCompat.from(requireActivity())

        val intent = Intent(requireActivity(), TabbedHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            requireActivity(), 0, intent, FLAG_MUTABLE
        )

        progressNotification = NotificationCompat.Builder(requireActivity(), App.PROGRESS_NOTIFICATION_ID)
            .setSmallIcon(R.drawable.ic_baseline_delete_sweep_24)
            .setContentTitle("Home")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)

        CoroutineScope(Dispatchers.Main).launch {
            if (!PlaylistService.playlistExists(App.getDB(), ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID)) {
                PlaylistService.addPlaylist(
                    App.getDB(), Playlist(
                        ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID,
                        resources.getString(R.string.loved_tracks_playlist_name),
                        0,
                        null,
                        ArrayList(),
                        0,
                        false,
                        0,
                        Date(),
                        Date()
                    )
                )
            }

            if (!PlaylistService.playlistExists(App.getDB(), ReservedPlaylists.DOWNLOADS_PLAYLIST_ID)) {
                PlaylistService.addPlaylist(
                    App.getDB(), Playlist(
                        ReservedPlaylists.DOWNLOADS_PLAYLIST_ID,
                        resources.getString(R.string.downloads_playlist_name),
                        0,
                        null,
                        ArrayList(),
                        1,
                        false,
                        0,
                        Date(),
                        Date()
                    )
                )
            }

            refreshPlaylists(recyclerLayout, requireActivity(), context)
        }

        itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(
                UP or
                DOWN or
                START or
                END, 0
            ) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    val adapter = recyclerView.adapter as PlaylistsListViewRecyclerAdapter
                    val from = viewHolder.adapterPosition
                    val to = target.adapterPosition

                    adapter.moveItem(from, to)
                    adapter.notifyItemMoved(from, to)

                    return true
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.absoluteAdapterPosition
                    val playlist = playlists[position]

                    when (direction) {
                        LEFT -> {
                            if (!ReservedPlaylists.isReserved(playlist.id)) {
                                deletePlaylist(playlist)
                            } else {
                                Toast.makeText(activity, "${playlist.name} cannot be deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    val position = viewHolder.absoluteAdapterPosition

                    return createSwipeFlags(position, recyclerView, viewHolder)
                }

                private fun createSwipeFlags(position: Int, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                    val playlist = playlists[position]

                    return if (ReservedPlaylists.isReserved(playlist.id)) {
                        0
                    } else {
                        super.getSwipeDirs(recyclerView, viewHolder)
                    }
                }

                override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                    super.onSelectedChanged(viewHolder, actionState)

                    if (actionState == ACTION_STATE_DRAG) {
                        swipeRefreshLayout.isEnabled = false
                        viewHolder?.itemView?.alpha = 0.7f
                    } else {
                        swipeRefreshLayout.isEnabled = true
                    }
                }

                override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                    super.clearView(recyclerView, viewHolder)

                    viewHolder.itemView.alpha = 1.0f
                }

            })

        itemTouchHelper.attachToRecyclerView(recyclerLayout)

        addPlaylistButton?.setOnClickListener {
            val builder = PlaylistBuilder.buildPlaylistDialog(requireActivity(), playlists, ::refreshPlaylistsWithItemAdded, null, null, null, null)

            builder.show()
        }

        swipeRefreshLayout?.setOnRefreshListener {
            Log.d("playlists_frag", "Refreshing...")

            refreshPlaylists(recyclerLayout, requireActivity(), this)

            swipeRefreshLayout.isRefreshing = false
        }

        return view
    }

    override fun onResume() {
        super.onResume()

        viewModel.selectItem("Home")
        requireView().findViewById<TextView>(R.id.greeting_text).text = greet()
    }

    private fun greet(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..5 -> {
                "Go to sleep!"
            }
            in 6..11 -> {
                "Good morning!"
            }
            in 12..16 -> {
                "Good afternoon!"
            }
            else -> "Good evening!"
        }
    }

    private fun refreshPlaylists(recyclerLayout: RecyclerView, activity: Activity, context: PlaylistFragment) {
        CoroutineScope(Dispatchers.Main).launch {
            playlists = PlaylistService.getAllPlaylistsWithoutTracks(App.getDB()) as ArrayList<Playlist>

            playlistsAdapter = PlaylistsListViewRecyclerAdapter(playlists, activity, App.getDB(), context)

            recyclerLayout.adapter = playlistsAdapter

            playlistsAdapter.notifyDataSetChanged()
        }
    }

    private fun refreshPlaylistsWithItemRemoved(playlist: Playlist) {
        val position = playlists.indexOf(playlist)

        playlists.removeAt(position)
        playlistsAdapter.notifyItemRemoved(position)
    }

    fun deletePlaylist(playlist: Playlist) {
        CoroutineScope(Dispatchers.Main).launch {
            val playlistRemovedSuccessfully = PlaylistService.deletePlaylists(App.getDB(), listOf(playlist))

            if (playlistRemovedSuccessfully) {
                refreshPlaylistsWithItemRemoved(playlist)

                Toast.makeText(activity, "Deleted ${playlist.name} successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Failed to delete ${playlist.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deletePlaylistAndTracks(playlist: Playlist) {
        CoroutineScope(Dispatchers.Main).launch {
            val playlistPopulatedWithTracksToDelete = PlaylistService.getPlaylistPopulatedWithTracks(App.getDB(), playlist.id)
            val numOfTracksToDelete = playlistPopulatedWithTracksToDelete.tracks.filter { it.location == ContainerLocation.LOCAL }.size

            progressNotification
                .setContentText("Deleting ${playlistPopulatedWithTracksToDelete.name} and $numOfTracksToDelete tracks...")
                .setProgress(maxProgress, 0, true)

            notificationManager.notify(playlistPopulatedWithTracksToDelete.id.hashCode(), progressNotification.build())

            val playlistRemovedSuccessfully = PlaylistService.deletePlaylistAndTracks(App.getDB(), playlistPopulatedWithTracksToDelete)

            if (playlistRemovedSuccessfully) {
                refreshPlaylistsWithItemRemoved(playlist)

                progressNotification.setContentText("Deleted ${playlistPopulatedWithTracksToDelete.name} and $numOfTracksToDelete tracks successfully")
                    .setProgress(0, maxProgress, false)
                    .setOngoing(false)

                notificationManager.notify(playlist.id.hashCode(), progressNotification.build())

                Toast.makeText(activity, "Deleted ${playlistPopulatedWithTracksToDelete.name} and $numOfTracksToDelete tracks successfully", Toast.LENGTH_SHORT).show()
            } else {
                progressNotification.setContentText("Failed to delete ${playlistPopulatedWithTracksToDelete.name} and $numOfTracksToDelete tracks")
                    .setProgress(0, maxProgress, false)
                    .setOngoing(false)

                notificationManager.notify(playlistPopulatedWithTracksToDelete.id.hashCode(), progressNotification.build())

                Toast.makeText(activity, "Failed to delete ${playlistPopulatedWithTracksToDelete.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshPlaylistsWithItemAdded(position: Int) {
        playlistsAdapter.notifyItemInserted(position)
    }
}
