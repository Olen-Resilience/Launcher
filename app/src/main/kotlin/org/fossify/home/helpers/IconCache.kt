package org.fossify.home.helpers

import android.graphics.drawable.Drawable
import org.fossify.home.models.AppLauncher

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    // Persistent drawable cache keyed by "packageName/activityName".
    // Survives refreshLaunchers() calls so icons are never re-decoded on resume.
    private val iconMap = HashMap<String, Drawable>()

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) {
                cachedLaunchers = value
            }
        }

    fun getIcon(identifier: String): Drawable? {
        return synchronized(this) { iconMap[identifier] }
    }

    fun putIcon(identifier: String, drawable: Drawable) {
        synchronized(this) { iconMap[identifier] = drawable }
    }

    fun clear() {
        synchronized(this) {
            launchers = emptyList()
            iconMap.clear()
        }
    }
}
