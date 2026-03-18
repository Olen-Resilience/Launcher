package org.fossify.home.helpers

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Disk-backed icon cache.
 *
 * Icons are stored as lossless PNG files inside [cacheDir].
 * The filename is derived from the launcher identifier
 * ("packageName/activityName") by replacing special characters.
 *
 * All I/O runs on the caller's thread — callers must ensure they are
 * already on a background thread before calling [save].
 */
class DiskIconCache(private val cacheDir: File, private val resources: Resources) {

    init {
        cacheDir.mkdirs()
    }

    /** Load a cached icon from disk. Returns null on any error or cache miss. */
    fun load(identifier: String): Drawable? {
        val file = fileFor(identifier)
        if (!file.exists()) return null
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            BitmapDrawable(resources, bitmap)
        } catch (_: Exception) {
            null
        }
    }

    /** Persist a drawable to disk. Must be called from a background thread. */
    fun save(identifier: String, drawable: Drawable) {
        try {
            val bitmap = drawable.toBitmap(
                width = maxOf(drawable.intrinsicWidth, 1),
                height = maxOf(drawable.intrinsicHeight, 1),
                config = Bitmap.Config.ARGB_8888
            )
            FileOutputStream(fileFor(identifier)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (_: Exception) {
            // Disk cache is best-effort; silently ignore all I/O failures.
        }
    }

    /** Delete all cached files whose name starts with the given package name. */
    fun evict(packageName: String) {
        try {
            val prefix = safeKey(packageName)
            cacheDir.listFiles()?.forEach { if (it.name.startsWith(prefix)) it.delete() }
        } catch (_: Exception) { }
    }

    /** Delete every file in the cache directory. */
    fun clear() {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) { }
    }

    private fun fileFor(identifier: String) = File(cacheDir, "${safeKey(identifier)}.png")

    // Replace characters that are illegal in filenames.
    private fun safeKey(value: String) = value.replace('/', '_').replace('.', '_')
}
