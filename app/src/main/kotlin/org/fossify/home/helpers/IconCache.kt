package org.fossify.home.helpers

import android.content.Context
import android.graphics.drawable.Drawable
import org.fossify.home.models.AppLauncher
import java.io.File
import java.util.concurrent.Executors

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    // L1: in-memory drawable cache — fast, lost on process death.
    private val iconMap = HashMap<String, Drawable>()

    // L1: in-memory placeholder colour cache.
    private val colorMap = HashMap<String, Int>()

    // L2: disk icon cache — survives process death, initialised in MainActivity.onCreate().
    @Volatile
    private var diskCache: DiskIconCache? = null

    // Single background thread for all disk saves so the caller is never blocked
    // and we never flood the thread pool with one thread per icon.
    private val saveExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "icon-disk-save").apply { isDaemon = true }
    }

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) { synchronized(this) { cachedLaunchers = value } }

    /**
     * Must be called once from MainActivity.onCreate() before any other method.
     * Creates the disk cache directory and wires up the DiskIconCache instance.
     */
    fun init(context: Context) {
        val dir = File(context.cacheDir, "icon_cache")
        diskCache = DiskIconCache(dir, context.resources)
    }

    /**
     * Returns the drawable for [identifier], checking L1 (memory) then L2 (disk).
     * On a disk hit the drawable is promoted to L1 for subsequent calls.
     */
    fun getIcon(identifier: String): Drawable? {
        // L1
        synchronized(this) { iconMap[identifier] }?.let { return it }
        // L2 — promote to L1 on hit
        return diskCache?.load(identifier)?.also { drawable ->
            synchronized(this) { iconMap[identifier] = drawable }
        }
    }

    /**
     * Stores [drawable] in L1 immediately and queues an async write to L2.
     * The caller need not be on any particular thread.
     */
    fun putIcon(identifier: String, drawable: Drawable) {
        synchronized(this) { iconMap[identifier] = drawable }
        val dc = diskCache ?: return
        saveExecutor.execute { dc.save(identifier, drawable) }
    }

    fun getColor(identifier: String): Int? =
        synchronized(this) { colorMap[identifier] }

    fun putColor(identifier: String, color: Int) {
        synchronized(this) { colorMap[identifier] = color }
    }

    /**
     * Removes all memory and disk cache entries for [packageName].
     * Called when a package is removed or replaced.
     */
    fun evictPackage(packageName: String) {
        synchronized(this) {
            val keys = iconMap.keys.filter { it.startsWith("$packageName/") }
            keys.forEach { iconMap.remove(it); colorMap.remove(it) }
        }
        diskCache?.evict(packageName)
    }

    fun clear() {
        synchronized(this) {
            cachedLaunchers = emptyList()
            iconMap.clear()
            colorMap.clear()
        }
    }
}
