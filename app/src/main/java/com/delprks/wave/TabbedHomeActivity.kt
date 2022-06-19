package com.delprks.wave

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.viewpager2.widget.ViewPager2
import com.delprks.wave.domain.LatestTrack
import com.delprks.wave.sections.SettingsFragment
import com.delprks.wave.sections.adapters.SectionsPagerAdapter
import com.delprks.wave.services.PlayerService
import com.delprks.wave.services.PlaylistService
import com.delprks.wave.util.Display
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import wave.R
import wave.databinding.ActivityTabbedHomeBinding
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

class TabbedHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTabbedHomeBinding

    private val viewModel: SharedTitle by viewModels()

    var playerService: PlayerService? = null

    private var mBound: Boolean = false

    private val activity = this

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            playerService = null
            mBound = false
        }
    }

    override fun onStart() {
        super.onStart()

        if (playerService == null) {
            Intent(this, PlayerService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onDestroy() {
        unbindService(connection)
        mBound = false

        super.onDestroy()
    }

    private fun setStatusBarGradiant(activity: Activity) {
        val window: Window = activity.window
        val background =
            ResourcesCompat.getDrawable(resources, R.drawable.main_header_selector, null)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = activity.resources.getColor(android.R.color.transparent)
        window.navigationBarColor = activity.resources.getColor(android.R.color.transparent)
        window.setBackgroundDrawable(background)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        if (savedInstanceState == null) {
//            supportFragmentManager.beginTransaction()
//                .add(R.id.sections_container, App.getPlaylistFragment(), "dashboard")
//                .commit()
//        }

        binding = ActivityTabbedHomeBinding.inflate(layoutInflater)

        window.navigationBarColor = resources.getColor(R.color.black)
        setStatusBarGradiant(this)
//        window.statusBarColor = resources.getColor(R.color.playlist_activity_start_background)

        setContentView(binding.root)

        val toolbar: Toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        viewModel.selectedItem.observe(this) { item ->
            toolbar.title = item
        }

        val downloadsPath = Path("$filesDir/Downloaded")

        if (Files.notExists(downloadsPath)) {
            val f = File(filesDir, "Downloaded")
            f.mkdir()
        }

        val player = findViewById<SlidingUpPanelLayout>(R.id.slidingUpPlayer)

        if (Display.isLandscape(resources)) {
            player.panelHeight = 0
        }

        // load latest played track/playlist
        CoroutineScope(Dispatchers.Main).launch {
            val latestTrack: LatestTrack? = PlaylistService.getLatestTrack(App.getDB())

            latestTrack?.let { trackStatus ->
                Log.d("home", "loaded track is $trackStatus")

                trackStatus.playlistId?.let { playlistId ->
                    if (PlaylistService.playlistExists(App.getDB(), playlistId)) {
                        val playlistWithTracks = PlaylistService.getPlaylistPopulatedWithTracks(App.getDB(), playlistId)
                        val latestTrackPlayedId = trackStatus.trackId
                        val tracks = playlistWithTracks.tracks.filter { it.id == latestTrackPlayedId }

                        if (tracks.isNotEmpty()) {
                            val track = tracks[0]
                            val position = playlistWithTracks.tracks.indexOf(track)

                            playerService?.loadSongs(activity, playlistWithTracks.tracks, playlistId, null, false)
                            playerService?.play(position, shuffled = trackStatus.shuffled, paused = true, initial = true)
                        } else {
                            player.panelHeight = 0
                        }
                    } else {
                        player.panelHeight = 0
                    }
                } ?: run {
                    player.panelHeight = 0
                }
            } ?: run {
                player.panelHeight = 0
            }
        }

        player.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(panel: View?, slideOffset: Float) {
                findViewById<RelativeLayout>(R.id.mini_player).alpha = 1 - slideOffset
                findViewById<RelativeLayout>(R.id.scrollable_player).visibility = View.VISIBLE
            }

            override fun onPanelStateChanged(
                panel: View?,
                previousState: SlidingUpPanelLayout.PanelState?,
                newState: SlidingUpPanelLayout.PanelState?
            ) {
//                if (newState == SlidingUpPanelLayout.PanelState.EXPANDED) {
//                    findViewById<NestedScrollView>(R.id.scrollable_player).visibility = View.GONE
//                }
//                else
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    findViewById<RelativeLayout>(R.id.scrollable_player).visibility = View.GONE
                }
            }
        })

        Intent(this, PlayerService::class.java).also { intent ->
            startService(intent)
        }

        val sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, lifecycle)
        val viewPager: ViewPager2 = binding.sectionsContainer

        // disables the animation between tabs
        viewPager.isUserInputEnabled = false
        viewPager.adapter = sectionsPagerAdapter

        val tabs: TabLayout = binding.tabs

//        val contentHeight = 600 //activity.window.findViewById<View>(Window.ID_ANDROID_CONTENT).run { bottom - top }
//        // 112dp is a deduction, 56dp for Toolbar and 56dp for BottomNavigationTab
//        val tabLayoutWidth =  contentHeight - dpToPx(activity,112).toInt()
//        tabs.layoutParams.width = 2400
//        tabs.layoutParams.height = 400
        // 44dp is basically half of assigned height[![enter image description here][2]][2]
//        tabs.translationX = (-90).toFloat()// (tabLayoutWidth / 2 - dpToPx(activity, 44)).toFloat() * -1
//        tabs.translationY = 0.toFloat() //(tabLayoutWidth / 2 - dpToPx(activity, 44)).toFloat()

        TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.icon = ResourcesCompat.getDrawable(this.resources, this.tabs[position].second, null)
            tab.customView = View.inflate(applicationContext, R.layout.custom_view_tab, null)
            tab.customView?.findViewById<TextView>(R.id.view_home_tab_text)?.text = this.resources.getString(this.tabs[position].first)
            tab.customView?.isSelected = tab.customView!!.isSelected
        }.attach()
    }

    private fun serviceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private val tabs = arrayOf(
        Pair(R.string.tab_title_1, R.drawable.tab_home),
        Pair(R.string.tab_title_2, R.drawable.tab_library),
        Pair(R.string.tab_title_3, R.drawable.tab_downloads)
    )

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> {
                CoroutineScope(Dispatchers.Main).launch {
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.main_layout, SettingsFragment())
                        .addToBackStack(null)
                        .commit()
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

}
