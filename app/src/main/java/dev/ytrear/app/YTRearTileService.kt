package dev.ytrear.app

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.app.NotificationManagerCompat

/**
 * Quick Settings tile that turns the rear-screen proxy on or off without opening the app.
 * A short tap toggles the same foreground service the main screen uses; long-press opens
 * YTRear. The tile stays inactive until notification access and a player selection exist.
 */
class YTRearTileService : TileService() {

    private val main = Handler(Looper.getMainLooper())

    /** HyperOS can deliver a tap twice; keep one physical tap to one toggle. */
    private var lastClickAt = 0L

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val now = SystemClock.elapsedRealtime()
        if (now - lastClickAt < 600L) return
        lastClickAt = now

        val running = RearControlService.isRunning
        Probe.log("TILE: click; ready=${isReady()} running=$running")
        if (!isReady()) {
            openApp()
            return
        }

        if (running) {
            RearControlService.stop(this)
        } else {
            RearControlService.start(this)
            if (MediaHub.attach(this)) {
                RearController.render(this, MediaHub.state, force = true)
            }
        }

        // The service flips isRunning asynchronously; refresh once it has settled.
        main.postDelayed({ updateTile() }, 400L)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val running = RearControlService.isRunning
        val ready = isReady()
        val label = PlayerSelection.targetLabel(this)
        val isAuto = PlayerSelection.isAuto(this)

        tile.state = if (ready && running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.app_name)
        tile.subtitle = when {
            !ready -> getString(R.string.tile_setup_needed)
            !running -> getString(R.string.tile_off)
            isAuto && label != null -> getString(R.string.tile_auto, label)
            label != null -> getString(R.string.tile_manual, label)
            else -> getString(R.string.tile_on)
        }
        tile.updateTile()
    }

    private fun isReady(): Boolean {
        val access = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        return access && PlayerSelection.isConfigured(this)
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            openAppLegacy(intent)
        }
    }

    /** The Intent overload still works on API 29-33; it is only unsupported from Android 14. */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun openAppLegacy(intent: Intent) {
        startActivityAndCollapse(intent)
    }
}
