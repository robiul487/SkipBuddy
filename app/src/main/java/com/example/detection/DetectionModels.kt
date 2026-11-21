package com.example.detection

data class NodeSnapshot(
    val nodeIndex: Int,
    val parentIndex: Int,
    val packageName: String,
    val className: String,
    val viewIdResourceName: String,
    val text: String,
    val contentDescription: String,
    val isVisibleToUser: Boolean,
    val isClickable: Boolean,
    val isEnabled: Boolean,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int
) {
    val width: Int get() = (boundsRight - boundsLeft).coerceAtLeast(0)
    val height: Int get() = (boundsBottom - boundsTop).coerceAtLeast(0)
    val hasValidBounds: Boolean get() = width > 0 && height > 0
}

enum class AdState {
    /** Normal video playback or non-ad YouTube UI. */
    NONE,
    /**
     * Sponsored card alone with no strong signal and no second independent weak signal.
     * Takes NO action (never mutes, never announces) so lingering Sponsored overlays
     * over a normal video never disturb watching.
     */
    OVERLAY_SPONSORED,
    /** Confirmed active advertisement session. */
    AD_ACTIVE
}

data class SkipCandidate(
    val nodeIndex: Int,
    val matchedBy: String,
    val isCountdownOnly: Boolean,
    val languageStep: String
)

data class ScanAnalysis(
    val adState: AdState,
    val strongSignals: List<String>,
    val weakSignals: List<String>,
    val skipCandidate: SkipCandidate?,
    val matchedLanguageStep: String?
) {
    val hasClickableSkip: Boolean
        get() = skipCandidate != null && !skipCandidate.isCountdownOnly
}
