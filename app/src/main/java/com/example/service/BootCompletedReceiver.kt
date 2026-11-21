package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.SkipBuddyRepository
import com.example.util.LocaleHelper
import com.example.util.NotificationHelper
import com.example.util.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Checks on phone restart whether SkipBuddy's Accessibility Service is still enabled.
 * Posts a quiet reminder notification only if it is turned OFF.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        if (PermissionChecker.isAccessibilityServiceEnabled(context)) {
            NotificationHelper.cancelServiceOffReminder(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = SkipBuddyRepository.getInstance(context).preferencesFlow.first()
                val localizedCtx = LocaleHelper.createLocalizedContext(context, prefs.appLanguage)
                NotificationHelper.showServiceOffReminder(localizedCtx)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
