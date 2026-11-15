package com.example.schoolproject.notifications

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.schoolproject.MainActivity
import com.example.schoolproject.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A daily, approximate reminder using Android's built-in alarm service. */
object ReminderScheduler {
    private const val CHANNEL = "daily_check_in"
    private const val REMINDER_ID = 100

    fun isAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        return Build.VERSION.SDK_INT < 26 || context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun sync(context: Context, enabled: Boolean, time: String) {
        context.getSharedPreferences("calma_reminder", Context.MODE_PRIVATE).edit()
            .putBoolean("enabled", enabled).putString("time", time).apply()
        val manager = context.getSystemService(AlarmManager::class.java)
        val pending = PendingIntent.getBroadcast(context, REMINDER_ID,
            Intent(context, ReminderReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.cancel(pending)
        if (!enabled || !isAllowed(context)) {
            NotificationManagerCompat.from(context).cancel(REMINDER_ID)
            return
        }
        val parsed = try { SimpleDateFormat("h:mm a", Locale.ENGLISH).apply { isLenient = false }.parse(time) }
            catch (_: Exception) { null } ?: return
        val clock = Calendar.getInstance().apply { this.time = parsed }
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, clock.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, clock.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.timeInMillis,
            AlarmManager.INTERVAL_DAY, pending)
    }

    fun restore(context: Context) {
        val prefs = context.getSharedPreferences("calma_reminder", Context.MODE_PRIVATE)
        sync(context, prefs.getBoolean("enabled", false), prefs.getString("time", "8:00 PM") ?: "8:00 PM")
    }

    fun show(context: Context) {
        val prefs = context.getSharedPreferences("calma_reminder", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("enabled", false) || !isAllowed(context)) return
        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Daily check-in", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("A moment for your daily check-in")
            .setContentText("Open Calma when you have a moment.")
            .setContentIntent(openApp).setAutoCancel(true).build()
        // The permission can change between the check and delivery.
        try { NotificationManagerCompat.from(context).notify(REMINDER_ID, notification) }
        catch (_: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { ReminderScheduler.show(context) }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED))
            ReminderScheduler.restore(context)
    }
}
