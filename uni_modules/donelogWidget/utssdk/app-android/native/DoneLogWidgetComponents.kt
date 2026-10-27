package uts.sdk.modules.donelogWidget

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import org.json.JSONArray
import uni.UNI7A2B275.updateWidgetViews
import java.util.Calendar

private const val PREF_NAME = "donelog_widget_pref"
private const val NOTIFICATION_SCHEDULES_KEY = "notification_schedules"
private const val NOTIFICATION_CHANNEL_ID = "donelog_event_reminders"
private const val NOTIFICATION_REQUEST_CODE_BASE = 32000

class DoneLogWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateWidgetViews(context.applicationContext)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        updateWidgetViews(context.applicationContext)
    }
}

class WidgetActionBridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventId = intent.getIntExtra("eventId", 0)
        val actionType = intent.getStringExtra("actionType").orEmpty().ifEmpty { "open-complete" }
        if (eventId > 0) {
            applicationContext
                .getSharedPreferences("donelog_widget_pref", Context.MODE_PRIVATE)
                .edit()
                .putString("pending_action", "{\"type\":\"$actionType\",\"eventId\":$eventId}")
                .apply()
        }

        packageManager.getLaunchIntentForPackage(packageName)?.let { launchIntent ->
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(launchIntent)
        }
        finish()
    }
}

class EventNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            updateWidgetViews(context.applicationContext)
            scheduleStoredNotifications(context.applicationContext)
            return
        }

        val eventId = intent.getIntExtra("eventId", 0)
        if (eventId <= 0) {
            return
        }

        val schedules = getStoredSchedules(context)
        for (index in 0 until schedules.length()) {
            val schedule = schedules.optJSONObject(index) ?: continue
            if (schedule.optInt("id") != eventId) {
                continue
            }

            showEventNotification(context, eventId, schedule.optString("name"))
            schedule.put("nextNotifyAt", getNextDailyNotifyAt(schedule.optString("notifyTime", "09:00")))
            saveStoredSchedules(context, schedules)
            scheduleAlarm(context, eventId, schedule.optLong("nextNotifyAt"))
            return
        }
    }

    private fun getStoredSchedules(context: Context): JSONArray {
        val stored = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(NOTIFICATION_SCHEDULES_KEY, "")
            .orEmpty()
        return try {
            if (stored.isEmpty()) JSONArray() else JSONArray(stored)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveStoredSchedules(context: Context, schedules: JSONArray) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(NOTIFICATION_SCHEDULES_KEY, schedules.toString())
            .apply()
    }

    private fun scheduleStoredNotifications(context: Context) {
        val schedules = getStoredSchedules(context)
        for (index in 0 until schedules.length()) {
            val schedule = schedules.optJSONObject(index) ?: continue
            val eventId = schedule.optInt("id")
            val triggerAt = schedule.optLong("nextNotifyAt")
            if (eventId > 0 && triggerAt > 0) {
                scheduleAlarm(context, eventId, triggerAt)
            }
        }
    }

    private fun scheduleAlarm(context: Context, eventId: Int, triggerAt: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildAlarmIntent(context, eventId)
        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms() ->
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ->
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
                else -> alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun buildAlarmIntent(context: Context, eventId: Int): PendingIntent {
        val intent = Intent(context, EventNotificationReceiver::class.java)
            .putExtra("eventId", eventId)
        return PendingIntent.getBroadcast(
            context,
            NOTIFICATION_REQUEST_CODE_BASE + eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showEventNotification(context: Context, eventId: Int, eventName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "事件提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val contentIntent = Intent(context, WidgetActionBridgeActivity::class.java)
            .putExtra("eventId", eventId)
            .putExtra("actionType", "open-detail")
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_REQUEST_CODE_BASE + eventId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            android.app.Notification.Builder(context)
        }
        val notification = builder
            .setSmallIcon(context.applicationInfo.icon)
            .setContentTitle("做完记 事件提醒")
            .setContentText("该完成：$eventName")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(NOTIFICATION_REQUEST_CODE_BASE + eventId, notification)
    }

    private fun getNextDailyNotifyAt(notifyTime: String): Long {
        val parts = notifyTime.split(':')
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return next.timeInMillis
    }
}
