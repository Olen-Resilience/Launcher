package org.fossify.home.helpers

import android.graphics.drawable.Drawable
import org.fossify.home.models.AppLauncher

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    // Drawable cache keyed by "packageName/activityName".
    private val iconMap = HashMap<String, Drawable>()

    // Placeholder colour cache — stores the result of calculateAverageColor()
    // so the expensive bitmap conversion and pixel scan only run once per app.
    private val colorMap = HashMap<String, Int>()

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) { cachedLaunchers = value }
        }

    fun getIcon(identifier: String): Drawable? =
        synchronized(this) { iconMap[identifier] }

    fun putIcon(identifier: String, drawable: Drawable) =
        synchronized(this) { iconMap[identifier] = drawable }

    fun getColor(identifier: String): Int? =
        synchronized(this) { colorMap[identifier] }

    fun putColor(identifier: String, color: Int) =
        synchronized(this) { colorMap[identifier] = color }

    fun evictPackage(packageName: String) {
        synchronized(this) {
            val keys = iconMap.keys.filter { it.startsWith("$packageName/") }
            keys.forEach {
                iconMap.remove(it)
                colorMap.remove(it)
            }
        }
    }

    fun clear() {
        synchronized(this) {
            cachedLaunchers = emptyList()
            iconMap.clear()
            colorMap.clear()
        }
    }
}
