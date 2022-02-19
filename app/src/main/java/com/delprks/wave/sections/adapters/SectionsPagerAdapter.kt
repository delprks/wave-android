package com.delprks.wave.sections.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.delprks.wave.App
import com.delprks.wave.sections.DownloadsFragment
import com.delprks.wave.sections.LibraryFragment

class SectionsPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle
) :
    FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            1 -> {
                LibraryFragment()
            }
            2 -> {
                DownloadsFragment()
            }
            else -> {
                App.getPlaylistFragment()
            }
        }
    }
}
