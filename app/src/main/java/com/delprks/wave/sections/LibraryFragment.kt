package com.delprks.wave.sections

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.delprks.wave.*
import com.delprks.wave.domain.Container
import com.delprks.wave.domain.ContainerLocation
import com.delprks.wave.domain.ContainerType
import com.delprks.wave.domain.TrackContainer
import com.delprks.wave.sections.adapters.LibraryListViewRecyclerAdapter
import com.delprks.wave.services.PlayerService
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.services.SettingsService
import com.delprks.wave.services.WebDavResourceRetriever
import com.delprks.wave.util.ReservedPlaylists
import com.delprks.wave.util.SupportedContent
import kotlinx.coroutines.*
import wave.R
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class LibraryFragment : Fragment() {

    private var remoteHostFullPath: String = ""
    private var remoteHostBasePath: String = ""
    private val db = App.getDB()
    private val viewModel: SharedTitle by activityViewModels()
    private var remoteContainers: ArrayList<Container> = ArrayList()
    private lateinit var previousContainer: Container
    private lateinit var adapter: LibraryListViewRecyclerAdapter
    private lateinit var lovedTracks: HashSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadSettings()
    }

    private fun loadSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            val currentSettings = SettingsService.getWebDavRemoteSettings(App.getDB())

            currentSettings?.let { settings ->
                remoteHostFullPath = settings.host + settings.mediaPath
                remoteHostBasePath = settings.host
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.selectItem("Library")
    }

    fun playerService(): PlayerService? {
        return (activity as TabbedHomeActivity).playerService
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.library_list, container)
        val recyclerLayout = view.findViewById<RecyclerView>(R.id.library_list)
        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLibraryLayout)
        val libraryFragment = this

        val callback = requireActivity().onBackPressedDispatcher.addCallback(this) {
            CoroutineScope(Dispatchers.Main).launch {
                if (::previousContainer.isInitialized) {
                    val explodedPath = previousContainer.path.split('/')
                    val name = explodedPath[explodedPath.size - 2]

                    remoteContainers = retrieveChildren(previousContainer.path, previousContainer.id, name)

                    adapter = LibraryListViewRecyclerAdapter(remoteContainers, requireActivity(), db, libraryFragment, lovedTracks)

                    recyclerLayout.adapter = adapter

                    adapter.notifyDataSetChanged()
                }
            }
        }

        callback.isEnabled = true

        CoroutineScope(Dispatchers.Main).launch {
            lovedTracks = PlaylistService.getPlaylistPopulatedWithTracks(db, ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID).tracks.map { it.id }.toHashSet()

            adapter = loadAdapterWithLibrary(libraryFragment)

            recyclerLayout.adapter = adapter
        }

        swipeRefreshLayout?.setOnRefreshListener {
            Log.d("playlists_frag", "Refreshing...")

            CoroutineScope(Dispatchers.Main).launch {
                loadSettings()

                lovedTracks = PlaylistService.getPlaylistPopulatedWithTracks(db, ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID).tracks.map { it.id }.toHashSet()

                adapter = loadAdapterWithLibrary(libraryFragment)

                recyclerLayout.adapter = adapter

                adapter.notifyDataSetChanged()

                swipeRefreshLayout.isRefreshing = false
            }
        }

        return view
    }

    suspend fun retrieveChildren(path: String, currentDirId: String, currentDirName: String): ArrayList<Container> {
        return withContext(Dispatchers.IO) {
            val resources = WebDavResourceRetriever.retrieve(requireActivity(), path)
            val containers = ArrayList<Container>()

            var startIndex = 0

            if (path != "$remoteHostFullPath/") {
                val name: String = if (currentDirId == "prev_dir") {
                    currentDirName.dropWhile { it == '<' || it == ' ' }
                } else {
                    currentDirName.dropLast(1)
                }

                val prevDir: String = if (currentDirId == "prev_dir") {
                    path.dropLastWhile { it == '/' }.dropLastWhile { it != '/' }
                } else {
                    path.dropLast(currentDirName.length)
                }

                previousContainer = Container(
                    "prev_dir",
                    "< $name",
                    prevDir,
                    ContainerType.DIRECTORY,
                    ContainerLocation.REMOTE,
                    0
                )

                containers.add(
                    previousContainer
                )

                startIndex = 1
            }

            for (i in startIndex until resources.size) {
                val res = resources[i]

                val resourceName = res.toString()

                if (resourceName.endsWith("/")) {
                    // it's a folder
                    val explodedResource = resourceName.split("/")
                    val name = explodedResource[explodedResource.size - 2] + "/"

                    containers.add(
                        Container(
                            resourceName,
                            name,
                            remoteHostBasePath + resourceName,
                            ContainerType.DIRECTORY,
                            ContainerLocation.REMOTE,
                            0
                        )
                    )
                } else if (SupportedContent.isValid(resourceName)) {
                    // it's a (valid) file
                    val explodedResource = resourceName.split("/")
                    val name = explodedResource[explodedResource.size - 1]

                    val loved = lovedTracks.contains(resourceName)
                    val currentDate = Date()

                    containers.add(
                        TrackContainer(
                            resourceName,
                            name,
                            remoteHostBasePath + resourceName,
                            ContainerLocation.REMOTE,
                            0,
                            null,
                            null,
                            null,
                            null,
                            loved,
                            null,
                            null,
                            currentDate,
                            currentDate
                        )
                    )
                }
            }

            if (path != "$remoteHostFullPath/") {
                val head = containers.removeAt(0)
                containers.sortBy { it.name }
                containers.add(0, head)
            } else {
                containers.sortBy { it.name }
            }

            containers
        }
    }

    private suspend fun loadAdapterWithLibrary(libraryFragment: LibraryFragment): LibraryListViewRecyclerAdapter {
        return withContext(Dispatchers.IO) {
            val resources = WebDavResourceRetriever.retrieve(requireActivity(), remoteHostFullPath)
            remoteContainers.clear()

            resources.forEach { res ->
                val resourceName = res.toString()

                if (resourceName.endsWith("/")) {
                    // it's a folder
                    val explodedResource = resourceName.split("/")
                    val name = explodedResource[explodedResource.size - 2] + "/"

                    remoteContainers.add(
                        Container(
                            resourceName,
                            name,
                            remoteHostBasePath + resourceName,
                            ContainerType.DIRECTORY,
                            ContainerLocation.REMOTE,
                            0
                        )
                    )
                } else if (SupportedContent.isValid(resourceName)) {
                    // it's a (valid) file
                    val explodedResource = resourceName.split("/")
                    val name = explodedResource[explodedResource.size - 1]

                    val loved = lovedTracks.contains(resourceName)
                    val currentDate = Date()

                    remoteContainers.add(
                        TrackContainer(
                            resourceName,
                            name,
                            remoteHostBasePath + resourceName,
                            ContainerLocation.REMOTE,
                            0,
                            null,
                            null,
                            null,
                            null,
                            loved,
                            null,
                            null,
                            currentDate,
                            currentDate
                        )
                    )
                }
            }

            remoteContainers.sortBy { it.name }

            LibraryListViewRecyclerAdapter(remoteContainers, requireActivity(), db, libraryFragment, lovedTracks)
        }
    }
}