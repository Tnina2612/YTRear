package dev.ytrear.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import dev.ytrear.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    /** Keeps the now-playing card live while the activity is in front. */
    private val mediaListener = MediaHub.Listener { runOnUiThread { refresh() } }
    private var observingMedia = false
    private var launchPickerPending = false
    private var playerPickerShowing = false
    private var startAfterNotificationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchPickerPending = savedInstanceState == null &&
            intent?.hasExtra("probe") != true &&
            !PlayerSelection.isAuto(this)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // targetSdk 36 always lays out edge to edge; keep the content clear of the
        // status bar and the gesture bar.
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }

        Probe.sink = Probe.Sink { full -> runOnUiThread { b.log.text = full } }
        b.log.text = Probe.text()

        b.btnChoosePlayer.setOnClickListener { showPlayerPicker() }
        b.btnExcludedPlayers.setOnClickListener { showExcludedPlayers() }
        b.btnSetupAccess.setOnClickListener { openNotificationAccess() }
        b.btnSetupBattery.setOnClickListener { openBatterySettings() }
        b.btnSetupAutostart.setOnClickListener { openAutostartSettings() }
        b.chkAutostart.setOnCheckedChangeListener { _, checked ->
            if (Setup.isAutostartConfirmed(this) == checked) return@setOnCheckedChangeListener
            Setup.setAutostartConfirmed(this, checked)
            refresh()
        }
        b.btnController.setOnClickListener { startSelectedPlayerProxy() }
        b.btnAdvanced.setOnClickListener { toggleAdvanced() }
        b.btnRefresh.setOnClickListener { refresh() }
        b.btnLanguage.setOnClickListener { showLanguagePicker() }

        refresh()
        handleProbeExtra(intent)
        if (launchPickerPending) b.root.post { showPlayerPicker() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasExtra("probe")) {
            handleProbeExtra(intent)
        } else if (!PlayerSelection.isAuto(this)) {
            showPlayerPicker()
        }
    }

    /** Lets the player mode be driven over adb: -e probe auto|manual */
    private fun handleProbeExtra(intent: Intent?) {
        when (intent?.getStringExtra("probe")) {
            "auto" -> applyPlayerMode(PlayerSelection.Mode.AUTO)
            "manual" -> applyPlayerMode(PlayerSelection.Mode.MANUAL)
        }
    }

    private fun applyPlayerMode(mode: PlayerSelection.Mode) {
        PlayerSelection.setMode(this, mode)
        MediaHub.targetChanged(this)
        MediaNotificationListener.targetChanged(this)
        startSelectedPlayerProxy()
        refresh()
    }

    /** One plain-language sentence for the status card, plus the dot colour behind it. */
    private data class Stage(
        @ColorRes val dot: Int,
        @StringRes val title: Int,
        @StringRes val detail: Int
    )

    private fun stage(access: Boolean, running: Boolean, playing: MediaHub.NowPlaying?): Stage {
        val isAuto = PlayerSelection.isAuto(this)
        return when {
            !PlayerSelection.isConfigured(this) ->
                Stage(R.color.state_bad, R.string.state_player_title, R.string.state_player_detail)
            !access -> Stage(
                R.color.state_bad,
                R.string.state_setup_title,
                if (isAuto) R.string.state_setup_detail_auto else R.string.state_setup_detail
            )
            !running -> Stage(R.color.state_wait, R.string.state_start_title, R.string.state_start_detail)
            playing == null -> Stage(
                R.color.state_wait,
                R.string.state_waiting_title,
                if (isAuto) R.string.state_waiting_detail_auto else R.string.state_waiting_detail
            )
            !NativeMediaProxy.active -> Stage(R.color.state_wait, R.string.state_linking_title, R.string.state_linking_detail)
            else -> Stage(R.color.state_ok, R.string.state_ready_title, R.string.state_ready_detail)
        }
    }

    private fun refresh() {
        val access = hasNotificationAccess()
        val running = RearControlService.isRunning
        val nowPlaying = MediaHub.state
        val isAuto = PlayerSelection.isAuto(this)
        val targetLabel = PlayerSelection.targetLabel(this)
        val batteryOk = isIgnoringBatteryOptimizations()
        val autostartOk = Setup.isAutostartConfirmed(this)

        val stage = stage(access, running, nowPlaying)
        b.statusDot.backgroundTintList = ColorStateList.valueOf(getColor(stage.dot))
        b.statusHeadline.setText(stage.title)
        b.statusDetail.setText(stage.detail)

        b.nowPlaying.isVisible = nowPlaying != null
        if (nowPlaying != null) {
            b.trackTitle.text = nowPlaying.title.ifBlank { getString(R.string.unknown_track) }
            val artist = nowPlaying.artist.ifBlank { getString(R.string.unknown_artist) }
            b.trackArtist.text = if (isAuto && targetLabel != null) "$artist · $targetLabel" else artist
            val art = nowPlaying.art
            if (art != null) b.art.setImageBitmap(art) else b.art.setImageDrawable(null)
        }

        // The warning only appears on a firmware build this release was not validated against.
        b.fwWarning.isVisible = !Firmware.isVerified()
        b.fwDetail.text = getString(
            R.string.firmware_warning_detail,
            Firmware.testedLabel,
            Firmware.label()
        )

        // Battery exemption and notification access can be read back; HyperOS autostart cannot,
        // so that row records the user's confirmation after they visit the system screen.
        b.setupCard.isVisible = !(access && batteryOk && autostartOk)

        b.setupAccessDot.backgroundTintList =
            ColorStateList.valueOf(getColor(if (access) R.color.state_ok else R.color.state_bad))
        b.setupAccessState.setText(
            if (access) R.string.setup_access_state_ok else R.string.setup_access_state_missing
        )
        b.btnSetupAccess.isVisible = !access

        b.setupBatteryDot.backgroundTintList =
            ColorStateList.valueOf(getColor(if (batteryOk) R.color.state_ok else R.color.state_wait))
        b.setupBatteryState.setText(
            if (batteryOk) R.string.setup_battery_state_ok else R.string.setup_battery_state_missing
        )
        b.btnSetupBattery.isVisible = !batteryOk

        b.setupAutostartDot.backgroundTintList =
            ColorStateList.valueOf(getColor(if (autostartOk) R.color.state_ok else R.color.state_wait))
        b.setupAutostartState.setText(
            if (autostartOk) R.string.setup_autostart_state_ok
            else R.string.setup_autostart_state_missing
        )
        if (b.chkAutostart.isChecked != autostartOk) b.chkAutostart.isChecked = autostartOk

        b.btnChoosePlayer.text = when {
            isAuto && targetLabel != null -> getString(R.string.auto_player_active, targetLabel)
            isAuto -> getString(R.string.auto_player_waiting)
            targetLabel != null -> getString(R.string.selected_player, targetLabel)
            else -> getString(R.string.choose_player)
        }
        b.btnExcludedPlayers.isVisible = isAuto
        if (isAuto) {
            val excludedCount = PlayerSelection.autoExcludedPackages(this).size
            b.btnExcludedPlayers.text = if (excludedCount == 0) {
                getString(R.string.excluded_apps_none)
            } else {
                getString(R.string.excluded_apps_count, excludedCount)
            }
        }
        b.btnController.isEnabled = PlayerSelection.isConfigured(this)
        b.btnController.setText(
            if (running) R.string.refresh_rear_controls else R.string.start_rear_controls
        )
        b.hint.setText(if (isAuto) R.string.daily_hint_auto else R.string.daily_hint)

        // Media callbacks arrive in bursts; only pay for the display enumeration when the
        // panel that shows it is actually open.
        if (b.advancedPanel.isVisible) refreshDiagnostics(access, nowPlaying)
    }

    /** The old probe read-outs, unchanged, now living behind the Advanced toggle. */
    private fun refreshDiagnostics(access: Boolean, nowPlaying: MediaHub.NowPlaying?) {
        val displays = Displays.all(this)
        b.displays.text = displays.joinToString("\n\n") { it.toString() }
        val rearDisplay = Displays.pickRear(this)
        b.status.text = buildString {
            append("Installed identity: $packageName\n")
            append(
                "Selected player: ${PlayerSelection.selectedLabel(this@MainActivity) ?: "NONE"} " +
                    "(${PlayerSelection.selectedPackage(this@MainActivity) ?: "none"})\n"
            )
            append("Player mode: ${PlayerSelection.mode(this@MainActivity).name}\n")
            append(
                "Effective target: ${PlayerSelection.targetLabel(this@MainActivity) ?: "NONE"} " +
                    "(${MediaHub.targetPackage ?: "none"})\n"
            )
            append("Excluded apps: ${PlayerSelection.autoExcludedPackages(this@MainActivity).size}\n")
            append("Notification access: ${if (access) "GRANTED" else "MISSING"}\n")
            append(
                "Battery optimization: " +
                    "${if (isIgnoringBatteryOptimizations()) "IGNORED" else "OPTIMIZED"}\n"
            )
            append("Autostart confirmed: ${if (Setup.isAutostartConfirmed(this@MainActivity)) "YES" else "NO"}\n")
            append("Firmware: ${Firmware.label()}\n")
            append("Firmware support: ${if (Firmware.isVerified()) "VERIFIED" else "UNVERIFIED"}\n")
            append("Media session access: ${if (MediaHub.connected) "CONNECTED" else "DISCONNECTED"}\n")
            append("Rear control service: ${if (RearControlService.isRunning) "RUNNING" else "STOPPED"}\n")
            append("Target session: ${nowPlaying?.title?.ifBlank { "active" } ?: "not active"}\n")
            append("Allowlisted proxy: ${if (NativeMediaProxy.active) "ACTIVE" else "INACTIVE"}\n")
            append("Displays: ${displays.size}\n")
            append("Rear pick: ${rearDisplay?.displayId ?: "none"}")
        }
    }

    private fun toggleAdvanced() {
        val show = !b.advancedPanel.isVisible
        b.advancedPanel.isVisible = show
        b.btnAdvanced.setText(if (show) R.string.hide_advanced else R.string.show_advanced)
        if (show) refresh()
    }

    private fun hasNotificationAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun openNotificationAccess() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    /**
     * HyperOS exposes autostart on a dedicated security-center screen. Other builds fall back to
     * the app info page, where some vendors place the same toggle.
     */
    private fun openAutostartSettings() {
        val candidates = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri()
            )
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next, less specific screen.
            } catch (_: SecurityException) {
                // The OEM activity exists but is not exported for this caller.
            }
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean =
        getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(packageName) == true

    /**
     * HyperOS freezes background processes unless the app is exempt from battery optimization.
     * Prefer the direct request dialog, then the optimization list, then the app info page where
     * HyperOS exposes its own "Battery saver" entry.
     */
    @SuppressLint("BatteryLife")
    private fun openBatterySettings() {
        val candidates = listOf(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:$packageName".toUri()
            ),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri()
            )
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the next, less specific screen.
            }
        }
    }

    private fun showPlayerPicker() {
        if (playerPickerShowing) return
        playerPickerShowing = true
        launchPickerPending = true
        PlayerPickerDialog.show(
            activity = this,
            onSelected = { selected ->
                playerPickerShowing = false
                launchPickerPending = false
                PlayerSelection.select(this, selected)
                MediaHub.targetChanged(this)
                MediaNotificationListener.targetChanged(this)
                startSelectedPlayerProxy()
                refresh()
            },
            onAutoSelected = {
                playerPickerShowing = false
                launchPickerPending = false
                applyPlayerMode(PlayerSelection.Mode.AUTO)
            },
            onCancelled = {
                playerPickerShowing = false
                launchPickerPending = false
                if (PlayerSelection.isConfigured(this)) startSelectedPlayerProxy()
                refresh()
            }
        )
    }

    private fun showExcludedPlayers() {
        if (playerPickerShowing) return
        playerPickerShowing = true
        PlayerPickerDialog.showExclusions(
            activity = this,
            onFinished = {
                playerPickerShowing = false
                MediaHub.targetChanged(this)
                MediaNotificationListener.targetChanged(this)
                if (PlayerSelection.isConfigured(this)) startSelectedPlayerProxy()
                refresh()
            }
        )
    }

    private fun startSelectedPlayerProxy() {
        if (!PlayerSelection.isConfigured(this)) {
            showPlayerPicker()
            return
        }
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            startAfterNotificationPermission = true
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
            return
        }
        startController()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS && startAfterNotificationPermission) {
            startAfterNotificationPermission = false
            startController()
        }
    }

    private fun startController() {
        if (!PlayerSelection.isConfigured(this)) {
            showPlayerPicker()
            return
        }
        if (!hasNotificationAccess()) {
            Probe.log("CONTROLLER: notification access is required")
            openNotificationAccess()
            return
        }

        RearControlService.start(this)
        if (MediaHub.attach(this)) {
            RearController.render(this, MediaHub.state, force = true)
            Probe.log("CONTROLLER: attached; rear widget refreshed")
        } else {
            Probe.log("CONTROLLER: could not read active media sessions")
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (
            !launchPickerPending &&
            !startAfterNotificationPermission &&
            PlayerSelection.isConfigured(this) &&
            hasNotificationAccess()
        ) {
            MediaNotificationListener.targetChanged(this)
            RearControlService.start(this)
            if (MediaHub.attach(this)) {
                RearController.render(this, MediaHub.state)
            }
        }
        if (!observingMedia) {
            MediaHub.addListener(mediaListener)
            observingMedia = true
        }
        refresh()
    }

    override fun onPause() {
        if (observingMedia) {
            MediaHub.removeListener(mediaListener)
            observingMedia = false
        }
        super.onPause()
    }

    override fun onDestroy() {
        Probe.sink = null
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 1
    }
}
