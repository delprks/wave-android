package com.delprks.wave.sections

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.delprks.wave.sections.adapters.DownloadListViewRecyclerAdapter
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.delprks.wave.*
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.services.PlayerService
import com.delprks.wave.services.PlaylistService
import kotlinx.coroutines.*
import wave.R

class DownloadsFragment : Fragment() {
    private val db: AppDatabase = App.getDB()
    private val viewModel: SharedTitle by activityViewModels()
    private lateinit var downloadsAdapter: DownloadListViewRecyclerAdapter
    private lateinit var downloads: ArrayList<TrackContainer>

    override fun onResume() {
        super.onResume()

        viewModel.selectItem("Downloads")
    }

    fun playerService(): PlayerService? {
        return (activity as TabbedHomeActivity).playerService
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.downloaded_list, container)
        val recyclerLayout: RecyclerView = view.findViewById(R.id.downloaded_list)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)

        val itemTouchHelper = ItemTouchHelper(
            object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    val fromPos = viewHolder.adapterPosition
                    val toPos = target.adapterPosition
                    // move item in `fromPos` to `toPos` in adapter.
                    return true // true if moved, false otherwise
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.absoluteAdapterPosition

                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            deleteDownloadedTrack(downloads[position])
                        }
                    }
                }
            })

        itemTouchHelper.attachToRecyclerView(recyclerLayout)

        refreshDownloads(recyclerLayout, activity!!, db, this)

        swipeRefreshLayout?.setOnRefreshListener {
            Log.d("download_frag", "Refreshing...")

            refreshDownloads(recyclerLayout, activity!!, db, this)

            swipeRefreshLayout.isRefreshing = false
        }

        return view
    }

    private fun refreshDownloads(recyclerLayout: RecyclerView, activity: Activity, db: AppDatabase, context: DownloadsFragment) {
        CoroutineScope(Dispatchers.Main).launch {
            downloads = PlaylistService.getTracksByLocation(db, ContainerLocation.LOCAL) as ArrayList<TrackContainer>

            downloadsAdapter = DownloadListViewRecyclerAdapter(downloads, activity, db, context)
            recyclerLayout.adapter = downloadsAdapter
            downloadsAdapter.notifyDataSetChanged()
        }
    }

    private fun refreshDownloadsWithItemRemoved(item: TrackContainer) {
        val position = downloads.indexOf(item)

        downloads.remove(item)
        downloadsAdapter.notifyItemRemoved(position)
    }

    fun deleteDownloadedTrack(track: TrackContainer) {
        CoroutineScope(Dispatchers.Main).launch {
            val deletedSuccessfully = PlaylistService.deleteTracks(db, listOf(track))

            if (deletedSuccessfully) {
                refreshDownloadsWithItemRemoved(track)

                Toast.makeText(activity, "Deleted ${track.name} successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Failed to delete ${track.name}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
