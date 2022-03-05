package com.delprks.wave.sections.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View.GONE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import com.delprks.wave.*
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.Playlist
import com.delprks.wave.sections.ModifyPlaylistDetailsFragment
import com.delprks.wave.sections.PlaylistFragment
import com.delprks.wave.sections.PlaylistDetailsFragment
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.ImageCache
import com.delprks.wave.util.ReservedPlaylists
import com.delprks.wave.util.TextFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wave.R
import wave.databinding.PlaylistsListItemBinding

class PlaylistsListViewRecyclerAdapter(
    private var playlists: List<Playlist>,
    private val parentActivity: Activity?,
    private val db: AppDatabase,
    private val context: PlaylistFragment?
) : RecyclerView.Adapter<PlaylistsListViewRecyclerAdapter.ViewHolder>() {
    private val adapter = this
    private var selectedPos = RecyclerView.NO_POSITION

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PlaylistsListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    @SuppressLint("StringFormatMatches", "ClickableViewAccessibility")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.playlistName.text = TextFormatter.shorten(playlist.name, 23)
        holder.playlistSize.text = parentActivity!!.resources.getString(R.string.playlist_item_count_txt, playlist.size)

        val playlistDetailsFragment = PlaylistDetailsFragment()

        when (playlist.id) {
            ReservedPlaylists.DOWNLOADS_PLAYLIST_ID -> {
                holder.playlistImage.setImageResource(R.drawable.downloads_playlist_cover)
                holder.playlistOptions.visibility = GONE
            }

            ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID -> {
                holder.playlistImage.setImageResource(R.drawable.loved_tracks_playlist_cover)
                holder.playlistOptions.visibility = GONE
            }

            else -> playlist.image?.let {
                holder.playlistImage.setImageURI(ImageCache.getUri(playlist.image!!))
            } ?: holder.playlistImage.setImageResource(R.drawable.cover)
        }

        val reorderButton = holder.itemView.findViewById<ImageView>(R.id.playlist_reorder)

        reorderButton.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                context?.itemTouchHelper?.startDrag(holder)
            }

            return@setOnTouchListener true
        }

        holder.itemView.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.bindingAdapterPosition
            notifyItemChanged(selectedPos)

            App.setCurrentPlaylist(playlists[selectedPos])

            CoroutineScope(Dispatchers.Main).launch {
                (parentActivity as TabbedHomeActivity).supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_layout, playlistDetailsFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        holder.itemView.isSelected = selectedPos == position

        holder.playlistOptions.setOnClickListener {
            val popup = PopupMenu(parentActivity, holder.playlistOptions)
            popup.inflate(R.menu.playlists_menu)

            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.playlist_edit_details -> {
                        if (!ReservedPlaylists.isReserved(playlist.id)) {
                            CoroutineScope(Dispatchers.Main).launch {
                                val modifyPlaylistDetailsFragment = ModifyPlaylistDetailsFragment(db, playlist, adapter, position)

                                (parentActivity as TabbedHomeActivity).supportFragmentManager
                                    .beginTransaction()
                                    .replace(R.id.main_layout, modifyPlaylistDetailsFragment)
                                    .addToBackStack(null)
                                    .commit()
                            }
                        }

                        true
                    }
                    R.id.playlist_remove -> {
                        if (!ReservedPlaylists.isReserved(playlist.id)) {
                            context?.deletePlaylist(playlist)
                        }

                        true
                    }
                    R.id.playlist_and_tracks_remove -> {
                        if (!ReservedPlaylists.isReserved(playlist.id)) {
                            Toast.makeText(
                                parentActivity,
                                "Deleting ${playlist.name} and associated tracks...",
                                Toast.LENGTH_SHORT
                            ).show()

                            context?.deletePlaylistAndTracks(playlist)
                        }

                        true
                    }
                    else -> {
                        false
                    }
                }
            }

            popup.show()
        }
    }

    override fun getItemCount(): Int = playlists.size

    fun moveItem(from: Int, to: Int) {
        val playlist = playlists[from]
        playlist.order = to

        val replacedPlaylist = playlists[to]
        replacedPlaylist.order = from

        CoroutineScope(Dispatchers.Main).launch {
            PlaylistService.updatePlaylist(db, playlist)
            PlaylistService.updatePlaylist(db, replacedPlaylist)

            playlists = playlists.sortedBy { it.order }
        }

    }

    inner class ViewHolder(binding: PlaylistsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val playlistName: TextView = binding.playlistName
        val playlistSize: TextView = binding.playlistItemCount
        val playlistImage: ImageView = binding.playlistImage
        val playlistOptions: TextView = binding.playlistOptions

        override fun toString(): String {
            return super.toString() + " '" + playlistName.text + "'"
        }
    }

}
