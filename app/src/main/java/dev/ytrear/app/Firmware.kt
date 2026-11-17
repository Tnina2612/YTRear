package dev.ytrear.app

import android.os.Build

/**
 * Identifies the firmware builds this release was validated against.
 *
 * The rear-widget path is firmware-specific, so a build running on an unverified HyperOS version
 * shows a warning instead of silently assuming identical behavior.
 */
object Firmware {

    /** HyperOS builds with recorded on-device validation. */
    private val VERIFIED = listOf(
        "OS3.0.318.0.WBLCNXM",
        "OS4.0.6.0.XBLCNXM"
    )

    /** Human-readable list of validated builds for the warning card. */
    val testedLabel: String
        get() = VERIFIED.joinToString(" / ")

    private val candidates: List<String>
        get() = listOf(Build.DISPLAY, Build.VERSION.INCREMENTAL, Build.ID)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()

    /** Human-readable build label for the warning card and diagnostics. */
    fun label(): String =
        candidates.firstOrNull { it.startsWith("OS", ignoreCase = true) }
            ?: candidates.firstOrNull()
            ?: "unknown"

    fun isVerified(): Boolean =
        candidates.any { candidate ->
            VERIFIED.any { build -> candidate.contains(build, ignoreCase = true) }
        }
}
