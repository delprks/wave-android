package com.delprks.wave.util

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import com.delprks.wave.*
import com.delprks.wave.domain.Container
import com.delprks.wave.domain.ContainerType
import com.delprks.wave.domain.LibraryAddToPlaylistType
import com.delprks.wave.domain.Playlist
import com.delprks.wave.services.PlaylistService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.StringUtils
import java.util.*
import kotlin.collections.ArrayList

object PlaylistBuilder {

    fun buildPlaylistDialog(
        activity: Activity,
        playlists: ArrayList<Playlist>,
        refreshPlaylistsWithItemAdded: ((position: Int) -> Unit)?,
        playlistActions: ((playlistId: Int, playlists: ArrayList<Playlist>, item: Container, additionType: LibraryAddToPlaylistType, position: Int, currentPlaylist: Playlist?) -> Unit)?,
        item: Container?,
        additionType: LibraryAddToPlaylistType?,
        position: Int?
    ): AlertDialog.Builder {
        val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle("Playlist title")

        val playlistNameInputBox = EditText(activity)
        playlistNameInputBox.inputType = InputType.TYPE_CLASS_TEXT

        if (item?.type == ContainerType.DIRECTORY) {
            val formattedDirName = when {
                item.id == "prev_dir" -> item.name.substring(2) // removes < at beginning
                item.name.endsWith("/") -> item.name.dropLast(1)
                else -> item.name
            }

            playlistNameInputBox.setText(formattedDirName)
        }

        playlistNameInputBox.setOnFocusChangeListener { _, _ ->
            playlistNameInputBox.post {
                val inputMethodManager: InputMethodManager = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(playlistNameInputBox, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        playlistNameInputBox.requestFocus()

        builder.setView(playlistNameInputBox)

        builder.setPositiveButton(
            "OK"
        ) { _, _ ->
            val newPlaylistName = playlistNameInputBox.text.trim().toString()
            val newPlaylistId = RandomStringUtils.randomNumeric(6)

            if (StringUtils.isNoneEmpty(newPlaylistName) && !ReservedPlaylists.isReserved(newPlaylistName)) {
                CoroutineScope(Dispatchers.Main).launch {
                    if (!PlaylistService.playlistExists(App.getDB(), newPlaylistId)) {
                        val newPlaylist = Playlist(
                            newPlaylistId,
                            newPlaylistName,
                            0,
                            null,
                            ArrayList(),
                            playlists.size,
                            false,
                            0,
                            Date(),
                            Date()
                        )

                        PlaylistService.addPlaylist(App.getDB(), newPlaylist)

                        Toast.makeText(
                            activity,
                            "Created playlist $newPlaylistName",
                            Toast.LENGTH_SHORT
                        ).show()

                        playlists.add(newPlaylist)

                        if (refreshPlaylistsWithItemAdded != null)
                            refreshPlaylistsWithItemAdded(playlists.size)

                        if (playlistActions != null)
                            playlistActions(Integer.parseInt(newPlaylistId), playlists, item!!, additionType!!, position!!, newPlaylist)
                    }
                }
            }
        }

        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.cancel() }

        return builder
    }
}
