package com.delprks.wave.util

import android.content.res.Configuration
import android.content.res.Resources

object Display {
    fun isPortrait(resources: Resources): Boolean {
       return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun isLandscape(resources: Resources): Boolean {
        return !isPortrait(resources)
    }
}
