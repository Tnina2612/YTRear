package dev.ytrear.app

import android.content.Context
import androidx.core.content.edit

/**
 * Setup state that Android does not expose to apps.
 *
 * Battery exemption and notification access can be read back from the system. HyperOS autostart
 * cannot, so it is recorded as a user confirmation after they visit the system screen.
 */
object Setup {

    private const val PREFS = "setup_state"
    private const val KEY_AUTOSTART_CONFIRMED = "autostart_confirmed"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAutostartConfirmed(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTOSTART_CONFIRMED, false)

    fun setAutostartConfirmed(context: Context, confirmed: Boolean) {
        prefs(context).edit { putBoolean(KEY_AUTOSTART_CONFIRMED, confirmed) }
    }
}
