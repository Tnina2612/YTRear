package dev.ytrear.app

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

/**
 * Loads launcher icons for the picker rows off the main thread and keeps a small in-memory
 * cache, so scrolling and repeated searches do not hit the PackageManager every time.
 */
object AppIcons {

    private val cache = object : LruCache<String, Drawable>(96) {}

    private val executor = Executors.newFixedThreadPool(2)

    private val main = Handler(Looper.getMainLooper())

    /**
     * Binds [packageName]'s launcher icon into [view]. A null package draws [fallback] (used for
     * the pinned auto-detect row). Results arriving after the row was recycled are discarded.
     */
    fun bind(view: ImageView, packageName: String?, fallback: Drawable?) {
        if (packageName == null) {
            view.tag = null
            view.setImageDrawable(fallback)
            return
        }

        view.tag = packageName
        val cached = cache.get(packageName)
        if (cached != null) {
            view.setImageDrawable(cached)
            return
        }

        view.setImageDrawable(fallback)
        val app = view.context.applicationContext
        executor.execute {
            val icon = loadIcon(app, packageName) ?: return@execute
            cache.put(packageName, icon)
            main.post {
                if (view.tag == packageName) view.setImageDrawable(icon)
            }
        }
    }

    private fun loadIcon(context: Context, packageName: String): Drawable? =
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
}
