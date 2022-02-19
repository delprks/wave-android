package com.delprks.wave.sections.adapters

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.delprks.wave.sections.LibraryFragment
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.FragmentActivity
import com.delprks.wave.*
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.services.WebDavDownloader
import com.delprks.wave.util.MetadataRetriever
import kotlinx.coroutines.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import com.delprks.wave.domain.*
import com.delprks.wave.util.ImageCache
import com.delprks.wave.util.PlaylistBuilder
import com.delprks.wave.util.ReservedPlaylists
import wave.R
import wave.databinding.LibraryListItemBinding
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class LibraryListViewRecyclerAdapter(
    private var remoteContainers: ArrayList<Container>,
    private val parentActivity: FragmentActivity,
    private val db: AppDatabase,
    private val context: LibraryFragment,
    private var lovedTracks: HashSet<String>
) : RecyclerView.Adapter<LibraryListViewRecyclerAdapter.ViewHolder>() {

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var progressNotification: NotificationCompat.Builder
    private val maxProgress = 100
    private var selectedPos = RecyclerView.NO_POSITION
    private val headerTextSize =
        parentActivity.resources.getDimension(R.dimen.header_item_text_size)

    @RequiresApi(Build.VERSION_CODES.M)
    private val headerTextColor = parentActivity.resources.getColor(R.color.primary, null)
    private val listItemTextSize =
        parentActivity.resources.getDimension(R.dimen.list_item_text_size)

    @RequiresApi(Build.VERSION_CODES.M)
    private val listItemTextColor = parentActivity.resources.getColor(R.color.light_white, null)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            LibraryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

        notificationManager = NotificationManagerCompat.from(parentActivity)

        val intent = Intent(parentActivity, TabbedHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            parentActivity, 0, intent, FLAG_MUTABLE
        )

        progressNotification =
            NotificationCompat.Builder(parentActivity, App.PROGRESS_NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_file_download)
                .setContentTitle("Library")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
//                                    .setAutoCancel(true)

        return holder
    }

    private fun destination(filename: String) = "${parentActivity.filesDir}/Downloaded/$filename"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = remoteContainers[position]
        holder.container.text = item.name

        // set the parent dir color
        if (item.id == "prev_dir") {
            holder.itemView.findViewById<TextView>(R.id.content)
                .setTextSize(TypedValue.COMPLEX_UNIT_PX, headerTextSize)
            holder.itemView.findViewById<TextView>(R.id.content).setTextColor(headerTextColor)
        } else {
            holder.itemView.findViewById<TextView>(R.id.content)
                .setTextSize(TypedValue.COMPLEX_UNIT_PX, listItemTextSize)
            holder.itemView.findViewById<TextView>(R.id.content).setTextColor(listItemTextColor)
        }

        // hide download icon if it's a dir change this to hide love icon
        if (item.type == ContainerType.DIRECTORY) {
            holder.itemView.findViewById<ImageView>(R.id.library_love).visibility = View.INVISIBLE
        } else {
            holder.itemView.findViewById<ImageView>(R.id.library_love).visibility = View.VISIBLE
        }

        if (item is TrackContainer) {
            if (item.loved) {
                holder.loveButton.setImageResource(R.drawable.loved_track)
            } else {
                holder.loveButton.setImageResource(R.drawable.unloved_track)
            }
        }

        holder.loveButton.setOnClickListener {
            if (item is TrackContainer) {
                CoroutineScope(Dispatchers.Main).launch {
                    val trackMetadata =
                        MetadataRetriever.getTrackMetadata(item.path, ContainerLocation.REMOTE)

                    item.loved = !item.loved

                    val trackContainer = TrackContainer(
                        item.id,
                        trackMetadata.title!!,
                        item.path,
                        item.location,
                        0,
                        trackMetadata.image,
                        trackMetadata.imageByteArray,
                        null,
                        trackMetadata.artist,
                        item.loved
                    )

                    PlaylistService.loveTrack(db, trackContainer)

                    if (item.loved) {
                        lovedTracks.add(item.id)
                    } else {
                        lovedTracks.remove(item.id)
                    }
                }

                notifyItemChanged(selectedPos)
                selectedPos = holder.bindingAdapterPosition
                notifyItemChanged(selectedPos)
                selectedPos = RecyclerView.NO_POSITION
            }
        }

        holder.itemView.setOnClickListener {
            notifyItemChanged(selectedPos)
            selectedPos = holder.bindingAdapterPosition
            notifyItemChanged(selectedPos)
            selectedPos = RecyclerView.NO_POSITION

            val path = remoteContainers[position].path
            val currentDirId = remoteContainers[position].id
            val currentDirName = remoteContainers[position].name
            val type = remoteContainers[position].type

            if (type == ContainerType.DIRECTORY) {
                CoroutineScope(Dispatchers.Main).launch {
                    val children: ArrayList<Container> = if (position == 0) {
                        val explodedPath = path.split('/')
                        val name = explodedPath[explodedPath.size - 2]

                        context.retrieveChildren(path, currentDirId, name)
                    } else {
                        context.retrieveChildren(path, currentDirId, currentDirName)
                    }

                    remoteContainers = children
                    notifyDataSetChanged()
                }
            } else {
                val tracks = remoteContainers.filter { it.type == ContainerType.FILE }
                tracks.forEach { t -> t.order = tracks.indexOf(t) }

                val trackPosition = tracks.indexOf(remoteContainers[position])

                val trackContainers = tracks.map { t ->
                    TrackContainer(
                        t.id,
                        t.name,
                        t.path,
                        t.location,
                        t.order,
                        null,
                        null,
                        null,
                        null
                    )
                }

                val dirName = trackContainers[0].path.dropLastWhile { it != '/' }

                context.playerService()?.loadSongs(parentActivity, trackContainers, dirName, null, false)
                context.playerService()?.play(trackPosition)
            }
        }

        holder.menuOptions.setOnClickListener {
            val popup = PopupMenu(parentActivity, holder.menuOptions)

            // show download for files only
            if (item is TrackContainer) {
                popup.menu.add(0, 0, 0, "Download")
            }

            val addToPlaylistSubMenu: SubMenu = popup.menu
                .addSubMenu(1, 1, 1, "Add to playlist")

            val downloadAndAddToPlaylistSubMenu: SubMenu = popup.menu
                .addSubMenu(2, 2, 2, "Download and add to playlist")

            CoroutineScope(Dispatchers.Main).launch {
                var playlists = PlaylistService.getAllPlaylistsWithoutTracks(db).filter {
                    !ReservedPlaylists.isReserved(it.id)
                } as ArrayList<Playlist>

                val createPlaylistText =
                    SpannableString(parentActivity.resources.getString(R.string.create_new_playlist_menu_item))
                createPlaylistText.setSpan(
                    StyleSpan(Typeface.BOLD),
                    0,
                    createPlaylistText.length,
                    0
                )

                addToPlaylistSubMenu.add(1, 3, 0, createPlaylistText).icon =
                    ResourcesCompat.getDrawable(
                        parentActivity.resources,
                        R.drawable.add_playlist,
                        null
                    )

                downloadAndAddToPlaylistSubMenu.add(2, 3, 0, createPlaylistText).icon =
                    ResourcesCompat.getDrawable(
                        parentActivity.resources,
                        R.drawable.add_playlist,
                        null
                    )

                if (playlists.isNotEmpty()) {
                    playlists.forEach { playlist ->
                        addToPlaylistSubMenu.add(1, Integer.parseInt(playlist.id), 0, playlist.name)
                        downloadAndAddToPlaylistSubMenu.add(
                            2,
                            Integer.parseInt(playlist.id),
                            0,
                            playlist.name
                        )
                    }
                }

                popup.inflate(R.menu.library_folder_menu)

                popup.setOnMenuItemClickListener {
                    when (it.itemId) {
                        0 -> { // download file
                            notifyItemChanged(selectedPos)
                            selectedPos = holder.bindingAdapterPosition
                            notifyItemChanged(selectedPos)
                            selectedPos = RecyclerView.NO_POSITION

                            CoroutineScope(Dispatchers.Main).launch {
                                val containerToDownload = remoteContainers[position]

                                if (containerToDownload.type == ContainerType.FILE) {
                                    progressNotification
                                        .setContentText("Downloading ${containerToDownload.name}...")
                                        .setProgress(maxProgress, 0, true)
                                    notificationManager.notify(
                                        containerToDownload.id.hashCode(),
                                        progressNotification.build()
                                    )

                                    when (downloadFile(containerToDownload)) {
                                        true -> {
                                            progressNotification.setContentText("Downloaded ${containerToDownload.name} successfully")
                                                .setProgress(0, maxProgress, false)
                                                .setOngoing(false)

                                            notificationManager.notify(
                                                containerToDownload.id.hashCode(),
                                                progressNotification.build()
                                            )

                                            Toast.makeText(
                                                parentActivity,
                                                "Downloaded ${containerToDownload.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        else -> {
                                            progressNotification.setContentText("Failed to download ${containerToDownload.name}")
                                                .setProgress(0, maxProgress, false)
                                                .setOngoing(false)

                                            notificationManager.notify(
                                                containerToDownload.id.hashCode(),
                                                progressNotification.build()
                                            )

                                            Toast.makeText(
                                                parentActivity,
                                                "Failed to download ${containerToDownload.name}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }

                            true
                        }
                        1 -> { // add to playlist
                            true
                        }
                        2 -> { // download and add to playlist
                            true
                        }
                        else -> {
                            val additionType: LibraryAddToPlaylistType = when (it.groupId) {
                                1 -> LibraryAddToPlaylistType.ADD_TO_PLAYLIST
                                2 -> LibraryAddToPlaylistType.DOWNLOAD_AND_ADD_TO_PLAYLIST
                                else -> throw IllegalArgumentException("Invalid action detected")
                            }

                            // create new playlist selected
                            if (it.itemId == 3) {
                                val builder = PlaylistBuilder.buildPlaylistDialog(
                                    parentActivity,
                                    playlists,
                                    null,
                                    ::playlistActions,
                                    item,
                                    additionType,
                                    position
                                )

                                builder.show()
                            } else {
                                playlistActions(
                                    it.itemId,
                                    playlists,
                                    item,
                                    additionType,
                                    position,
                                    null
                                )
                            }

                            true
                        }
                    }
                }

                popup.show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun playlistActions(
        playlistId: Int,
        playlists: ArrayList<Playlist>,
        item: Container,
        additionType: LibraryAddToPlaylistType,
        position: Int,
        currentPlaylist: Playlist?
    ) {
        val playlist: Playlist = currentPlaylist ?: playlists.find { playlist -> playlist.id == playlistId.toString() }!!
        var trackIdToUseForImage: String? = null

        CoroutineScope(Dispatchers.Main).launch {
            if (item.type == ContainerType.FILE) {
                if (additionType == LibraryAddToPlaylistType.ADD_TO_PLAYLIST) {
                    progressNotification
                        .setContentText("Adding ${item.name} to ${playlist.name}...")
                        .setProgress(maxProgress, 0, true)
                    notificationManager.notify(item.id.hashCode(), progressNotification.build())

                    when (addToPlaylist(playlist, item, ContainerLocation.REMOTE)) {
                        true -> {
                            progressNotification.setContentText("Added ${item.name} to ${playlist.name} successfully")
                                .setProgress(0, maxProgress, false)
                                .setOngoing(false)

                            notificationManager.notify(
                                item.id.hashCode(),
                                progressNotification.build()
                            )

                            trackIdToUseForImage = item.name.hashCode().toString()
                        }
                        else -> {
                            progressNotification.setContentText("Failed to add ${item.name} to ${playlist.name}")
                                .setProgress(0, maxProgress, false)
                                .setOngoing(false)

                            notificationManager.notify(
                                item.id.hashCode(),
                                progressNotification.build()
                            )
                        }
                    }

                    Toast.makeText(
                        parentActivity,
                        "Added ${item.name} to ${playlist.name} playlist",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    progressNotification
                        .setContentText("Downloading ${item.name}...")
                        .setProgress(maxProgress, 0, true)
                    notificationManager.notify(item.id.hashCode(), progressNotification.build())

                    val backgroundTask = if (downloadFile(remoteContainers[position])) {
                        addToPlaylist(playlist, item, ContainerLocation.LOCAL)
                    } else false

                    when (backgroundTask) {
                        true -> {
                            progressNotification.setContentText("Downloaded ${item.name} successfully")
                                .setProgress(0, maxProgress, false)
                                .setOngoing(false)

                            notificationManager.notify(
                                item.id.hashCode(),
                                progressNotification.build()
                            )

                            Toast.makeText(
                                parentActivity,
                                "Downloaded and added ${item.name} to ${playlist.name} playlist",
                                Toast.LENGTH_SHORT
                            ).show()

                            trackIdToUseForImage = item.name.hashCode().toString()
                        }
                        else -> {
                            progressNotification.setContentText("Failed to download ${item.name}")
                                .setProgress(0, maxProgress, false)
                                .setOngoing(false)

                            notificationManager.notify(
                                item.id.hashCode(),
                                progressNotification.build()
                            )

                            Toast.makeText(
                                parentActivity,
                                "Did not download/add ${item.name} to ${playlist.name} playlist",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }
            } else {
                val tracks = (
                        if (item.id == "prev_dir")
                            remoteContainers
                        else
                            context.retrieveChildren(item.path, item.id, item.name)
                        ).filter { container -> container.type == ContainerType.FILE }

                val trackCount = tracks.size

                if (additionType == LibraryAddToPlaylistType.ADD_TO_PLAYLIST) {
                    val notificationId =
                        LibraryAddToPlaylistType.ADD_TO_PLAYLIST.hashCode() + tracks.hashCode() + playlistId.hashCode()

                    progressNotification
                        .setContentText("Adding $trackCount tracks to ${playlist.name}...")
                        .setProgress(maxProgress, 0, false)

                    notificationManager.notify(notificationId, progressNotification.build())

                    var count = 1

                    val failedPlaylistAdditions: HashSet<String> = HashSet()

                    val result = tracks.map { track ->
                        progressNotification
                            .setContentTitle("($count/$trackCount)")
                            .setContentText("Adding ${track.name}...")
                            .setProgress(maxProgress, count * 100 / trackCount, false)

                        notificationManager.notify(notificationId, progressNotification.build())

                        count++

                        val result = addToPlaylist(playlist, track, ContainerLocation.REMOTE)

                        if (trackIdToUseForImage == null && result) {
                            trackIdToUseForImage = track.name.hashCode().toString()
                        }

                        if (!result) {
                            failedPlaylistAdditions.add(track.name)
                        }

                        result
                    }

                    val successes = result.filter { success -> success }.size
                    val failures = failedPlaylistAdditions.size

                    val notificationMessage = if (failures > 0) {
                        var counter = 0
                        val failedTracks = StringJoiner("\n")

                        failedPlaylistAdditions.forEach { track ->
                            counter++
                            failedTracks.add("$counter. $track")
                        }

                        "Added $successes tracks to ${playlist.name}, with $failures failures:\n$failedTracks"
                    } else {
                        "Added $successes tracks to ${playlist.name}, with $failures failures"
                    }

                    progressNotification
                        .setContentTitle("Library")
                        .setContentText(notificationMessage)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
                        .setProgress(0, maxProgress, false)
                        .setOngoing(false)

                    notificationManager.notify(notificationId, progressNotification.build())

                    Toast.makeText(
                        parentActivity,
                        "Added $successes tracks to ${playlist.name} playlist, with $failures failures",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val notificationId =
                        LibraryAddToPlaylistType.DOWNLOAD_AND_ADD_TO_PLAYLIST.hashCode() + tracks.hashCode() + playlistId.hashCode()

                    progressNotification
                        .setContentText("Downloading $trackCount tracks...")
                        .setProgress(maxProgress, 0, false)

                    notificationManager.notify(notificationId, progressNotification.build())

                    var count = 1

                    val failedDownloads: HashSet<String> = HashSet()

                    val result = tracks.map { track ->
                        progressNotification
                            .setContentTitle("($count/$trackCount)")
                            .setContentText("Downloading ${track.name}...")
                            .setProgress(maxProgress, count * 100 / trackCount, false)

                        notificationManager.notify(notificationId, progressNotification.build())

                        count++

                        val result = if (downloadFile(track)) {
                            addToPlaylist(playlist, track, ContainerLocation.LOCAL)
                        } else false

                        if (trackIdToUseForImage == null && result) {
                            trackIdToUseForImage = track.name.hashCode().toString()
                        }

                        if (!result) {
                            failedDownloads.add(track.name)
                        }

                        result
                    }

                    val successes = result.filter { success -> success }.size
                    val failures = failedDownloads.size

                    val notificationMessage = if (failures > 0) {
                        var counter = 0
                        val failedTracks = StringJoiner("\n")

                        failedDownloads.forEach { track ->
                            counter++
                            failedTracks.add("$counter. $track")
                        }

                        "Downloaded $successes tracks, with $failures failures:\n$failedTracks"
                    } else {
                        "Downloaded $successes tracks, with $failures failures"
                    }

                    progressNotification
                        .setContentTitle("Library")
                        .setContentText(notificationMessage)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
                        .setProgress(0, maxProgress, false)
                        .setOngoing(false)

                    notificationManager.notify(notificationId, progressNotification.build())

                    Toast.makeText(
                        parentActivity,
                        "Downloaded and added $successes tracks to ${playlist.name} playlist, with $failures failures",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // use the track image as the playlist image
            if (trackIdToUseForImage != null) {
                currentPlaylist?.let {
                    // TODO (dp-15.01.22) :: Avoid db call
                    val trackToUseForPlaylistImage = PlaylistService.getTrackById(db, trackIdToUseForImage!!)

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
        }
    }

    private suspend fun downloadFile(container: Container): Boolean {
        return withContext(Dispatchers.IO) {
            val downloadName = container.name
            val source = container.path
            val destination = destination(downloadName)
            var result = true

            if (!File(destination).exists()) {
                Log.d(TAG, "Downloading $destination")

                if (WebDavDownloader.download(parentActivity, source, destination)) {
                    val trackMetadata =
                        MetadataRetriever.getTrackMetadata(destination, ContainerLocation.LOCAL)

                    val title = if (trackMetadata.title == null) "Unknown track" else trackMetadata.title

                    val newTrack = TrackContainer(
                        downloadName.hashCode().toString(),
                        title!!,
                        destination,
                        ContainerLocation.LOCAL,
                        0,
                        trackMetadata.image,
                        trackMetadata.imageByteArray,
                        null,
                        trackMetadata.artist
                    )

                    val tracksAddedToPlaylist = PlaylistService.addTracksToPlaylist(
                        db,
                        ReservedPlaylists.DOWNLOADS_PLAYLIST_ID,
                        listOf(newTrack)
                    )

                    withContext(Dispatchers.Main) {
                        result = tracksAddedToPlaylist
                    }
                } else {
                    result = false
                }
            }

            result
        }
    }

    private suspend fun addToPlaylist(
        playlist: Playlist,
        track: Container,
        location: ContainerLocation
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val trackName = track.name
            val path =
                if (location == ContainerLocation.LOCAL) destination(trackName) else track.path
            val trackMetadata = MetadataRetriever.getTrackMetadata(path, location)

            val title = if (trackMetadata.title == null) "Unknown track" else trackMetadata.title

            val trackContainer = TrackContainer(
                trackName.hashCode().toString(),
                title!!,
                path,
                location,
                0,
                trackMetadata.image,
                trackMetadata.imageByteArray,
                null,
                trackMetadata.artist
            )

            PlaylistService.addTracksToPlaylist(db, playlist.id, listOf(trackContainer))
        }
    }

    override fun getItemCount(): Int = remoteContainers.size

    inner class ViewHolder(binding: LibraryListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val container: TextView = binding.content
        val loveButton: ImageView = binding.libraryLove
        val menuOptions: TextView = binding.libraryOptions

        override fun toString(): String {
            return super.toString() + " '" + container.text + "'"
        }
    }

}
