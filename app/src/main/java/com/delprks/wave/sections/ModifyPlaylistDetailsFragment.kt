package com.delprks.wave.sections

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import androidx.core.widget.NestedScrollView
import com.delprks.wave.*
import com.delprks.wave.dao.AppDatabase
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.ReservedPlaylists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior

import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.activity.result.contract.ActivityResultContracts
import com.delprks.wave.domain.Playlist
import com.delprks.wave.sections.adapters.PlaylistsListViewRecyclerAdapter
import com.delprks.wave.util.ImageCache
import wave.R

class ModifyPlaylistDetailsFragment(
    private val db: AppDatabase,
    private val playlist: Playlist,
    private val adapter: PlaylistsListViewRecyclerAdapter,
    private val position: Int
) : BottomSheetDialogFragment() {

    private var updatedPlaylistImage: Uri? = null
    private lateinit var context: Activity

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data

            updatedPlaylistImage = uri

            view?.findViewById<ImageView>(R.id.playlist_details_modify_playlist_image)?.setImageURI(uri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        context = requireActivity()

        val view = inflater.inflate(R.layout.playlist_details_modify, container, false)

        view.findViewById<ImageView>(R.id.playlist_details_modify_close_button).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
            // activity.onBackPressed() alt
        }

        val bottomSheet = view.findViewById<NestedScrollView>(R.id.playlist_details_modify_layout)
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
                activity?.findViewById<NestedScrollView>(R.id.playlist_details_modify_layout)?.alpha = 1 + v
            }
        })

        context.window.statusBarColor = resources.getColor(R.color.playlist_activity_start_background)
        context.actionBar?.hide()

        val playlistNameFieldValue = view.findViewById<TextView>(R.id.playlist_details_modify_playlist_name_value)
        val playlistImageView = view.findViewById<ImageView>(R.id.playlist_details_modify_playlist_image)

        CoroutineScope(Dispatchers.Main).launch {
            when {
                playlist.id == ReservedPlaylists.DOWNLOADS_PLAYLIST_ID -> {
                    playlistImageView.setImageResource(R.drawable.downloads_playlist_cover)
                }

                playlist.id == ReservedPlaylists.LOVED_TRACKS_PLAYLIST_ID -> {
                    playlistImageView.setImageResource(R.drawable.loved_tracks_playlist_cover)
                }

                StringUtils.isEmpty(playlist.image) -> {
                    playlistImageView.setImageResource(R.drawable.cover)
                }

                else -> playlist.image?.let {
                    playlistImageView.setImageURI(ImageCache.getUri(playlist.image!!))
                } ?: playlistImageView.setImageResource(R.drawable.cover)
            }

            playlistNameFieldValue.text = playlist.name
        }

        playlistImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            getContent.launch(intent)
        }

        val saveButton = view.findViewById<Button>(R.id.playlist_details_modify_save_btn)

        saveButton?.setOnClickListener {
            val playlistName = playlistNameFieldValue.text
            val playlistImage = updatedPlaylistImage?.let { playlistImageUrl ->
                val image = MediaStore.Images.Media.getBitmap(activity?.contentResolver, playlistImageUrl)
                val imageName = "${playlist.id}_cover"
                App.getImageCache().saveToCacheAndGetUri(image, imageName).path

                ImageCache.getName(imageName)
            } ?: playlist.image

            playlist.image = playlistImage

            if (!playlistName.isNullOrEmpty()) {
                CoroutineScope(Dispatchers.Main).launch {
                    playlist.name = playlistName.toString()

                    PlaylistService.updatePlaylist(db, playlist)

                    Toast.makeText(activity, "Saved $playlistName", Toast.LENGTH_SHORT).show()

                    // hide keyboard
                    val imm: InputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view!!.windowToken, 0)

                    requireActivity().supportFragmentManager.popBackStack()
                    adapter.notifyItemChanged(position)
                }
            } else {
                Toast.makeText(activity, "Playlist name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
