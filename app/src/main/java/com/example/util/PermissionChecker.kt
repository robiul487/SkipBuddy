package com.example.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.model.PermissionStatusState
import com.example.service.SkipBuddyAccessibilityService

object PermissionChecker {

    fun checkAll(context: Context): PermissionStatusState {
        return PermissionStatusState(
            accessibilityGranted = isAccessibilityServiceEnabled(context),
            overlayGranted = canDrawOverlays(context),
            batteryOptimizationIgnored = isIgnoringBatteryOptimizations(context),
            notificationsGranted = areNotificationsGranted(context)
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, SkipBuddyAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val componentStr = splitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentStr)
            if (enabledComponent != null && enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }

    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun areNotificationsGranted(context: Context): Boolean {
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabled) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(context, intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startSafely(context, intent)) {
            openAppDetailsSettings(context)
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        val directIntent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (!startSafely(context, directIntent)) {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (!startSafely(context, fallback)) {
                openAppDetailsSettings(context)
            }
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        if (!startSafely(context, intent)) {
            openAppDetailsSettings(context)
        }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(context, intent)
    }

    /**
     * Attempts brand-specific Xiaomi/Samsung autostart settings, gracefully falling back
     * to SkipBuddy's App Info screen so the user can adjust Autostart / Unrestricted battery.
     */
    fun openAutostartOrAppInfo(context: Context) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val candidates = mutableListOf<Intent>()
        if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco")) {
            candidates.add(
                Intent().setComponent(
                    ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                )
            )
        }
        for (candidate in candidates) {
            candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (startSafely(context, candidate)) return
        }
        openAppDetailsSettings(context)
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
