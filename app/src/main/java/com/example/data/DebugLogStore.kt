package com.example.data

import android.content.Context
import com.example.detection.NodeSnapshot
import com.example.detection.ScanAnalysis
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local-only ring-buffer log for YouTube ad moments when Detection Debug Log is turned ON.
 * Strictly logs ONLY the 9 permitted accessibility node fields:
 * packageName, className, viewIdResourceName, text, contentDescription,
 * isVisibleToUser, isClickable, isEnabled, boundsInScreen.
 */
object DebugLogStore {

    private const val LOG_FILE_NAME = "skipbuddy_detection_debug.txt"
    private const val MAX_FILE_BYTES = 80_000
    private val lock = Any()

    fun appendAdMoment(
        context: Context,
        eventLabel: String,
        analysis: ScanAnalysis,
        relevantNodes: List<NodeSnapshot>
    ) {
        synchronized(lock) {
            runCatching {
                val file = File(context.filesDir, LOG_FILE_NAME)
                val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
                val sb = StringBuilder()
                sb.append("=== [$ts] $eventLabel | adState=${analysis.adState} ")
                sb.append("strong=${analysis.strongSignals} weak=${analysis.weakSignals} ")
                sb.append("skip=${analysis.skipCandidate?.matchedBy ?: "none"} ")
                sb.append("langStep=${analysis.matchedLanguageStep ?: "none"} ===\n")
                relevantNodes.take(30).forEach { n ->
                    sb.append("  pkg=").append(n.packageName)
                        .append(" | cls=").append(n.className)
                        .append(" | id=").append(n.viewIdResourceName)
                        .append(" | text=").append(n.text.replace("\n", " "))
                        .append(" | desc=").append(n.contentDescription.replace("\n", " "))
                        .append(" | vis=").append(n.isVisibleToUser)
                        .append(" | clk=").append(n.isClickable)
                        .append(" | en=").append(n.isEnabled)
                        .append(" | bounds=[")
                        .append(n.boundsLeft).append(",")
                        .append(n.boundsTop).append(",")
                        .append(n.boundsRight).append(",")
                        .append(n.boundsBottom).append("]\n")
                }
                sb.append("\n")

                val existing = if (file.exists()) file.readText() else ""
                val combined = existing + sb.toString()
                val trimmed = if (combined.length > MAX_FILE_BYTES) {
                    combined.takeLast(MAX_FILE_BYTES)
                } else {
                    combined
                }
                file.writeText(trimmed)
            }
        }
    }

    fun readLog(context: Context): String {
        synchronized(lock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            return if (file.exists()) file.readText() else ""
        }
    }

    fun clearLog(context: Context) {
        synchronized(lock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists()) file.delete()
        }
    }
}
