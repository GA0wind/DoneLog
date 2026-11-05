package uts.sdk.modules.donelogWidget

import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import org.json.JSONArray
import uni.UNI7A2B275.updateWidgetViews
import java.util.Calendar

private const val PREF_NAME = "donelog_widget_pref"
private const val NOTIFICATION_SCHEDULES_KEY = "notification_schedules"
private const val SHIFT_SCHEDULES_KEY = "shift_notification_schedules"
private const val SHIFT_ALARM_ACTION = "donelog.shift.alarm"
private const val NOTIFICATION_CHANNEL_ID = "donelog_event_reminders"
private const val NOTIFICATION_REQUEST_CODE_BASE = 32000
private const val SHIFT_REQUEST_CODE_BASE = 132000

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
            scheduleStoredShiftNotifications(context.applicationContext)
            return
        }

        if (intent.action == SHIFT_ALARM_ACTION) {
            val shiftId = intent.getIntExtra("shiftId", 0)
            val schedules = getStoredShiftSchedules(context)
            for (index in 0 until schedules.length()) {
                val schedule = schedules.optJSONObject(index) ?: continue
                if (schedule.optInt("id") != shiftId) continue
                showShiftNotification(context, shiftId, schedule.optString("name"))
                schedules.remove(index)
                saveStoredShiftSchedules(context, schedules)
                return
            }
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

    private fun getStoredShiftSchedules(context: Context): JSONArray {
        val stored = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(SHIFT_SCHEDULES_KEY, "")
            .orEmpty()
        return try {
            if (stored.isEmpty()) JSONArray() else JSONArray(stored)
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun saveStoredShiftSchedules(context: Context, schedules: JSONArray) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(SHIFT_SCHEDULES_KEY, schedules.toString())
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

    private fun scheduleStoredShiftNotifications(context: Context) {
        val schedules = getStoredShiftSchedules(context)
        for (index in 0 until schedules.length()) {
            val schedule = schedules.optJSONObject(index) ?: continue
            val shiftId = schedule.optInt("id")
            val triggerAt = schedule.optLong("shiftAt")
            if (shiftId > 0 && triggerAt > System.currentTimeMillis()) {
                scheduleAt(context, triggerAt, buildShiftAlarmIntent(context, shiftId))
            }
        }
    }

    private fun scheduleAlarm(context: Context, eventId: Int, triggerAt: Long) {
        scheduleAt(context, triggerAt, buildAlarmIntent(context, eventId))
    }

    private fun scheduleAt(context: Context, triggerAt: Long, pendingIntent: PendingIntent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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

    private fun buildShiftAlarmIntent(context: Context, shiftId: Int): PendingIntent {
        val intent = Intent(context, EventNotificationReceiver::class.java)
            .setAction(SHIFT_ALARM_ACTION)
            .putExtra("shiftId", shiftId)
        return PendingIntent.getBroadcast(
            context,
            SHIFT_REQUEST_CODE_BASE + shiftId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
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

    private fun showShiftNotification(context: Context, shiftId: Int, shiftName: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(NOTIFICATION_CHANNEL_ID, "事件提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            SHIFT_REQUEST_CODE_BASE + shiftId,
            Intent(context, WidgetActionBridgeActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            android.app.Notification.Builder(context)
        }
        notificationManager.notify(
            SHIFT_REQUEST_CODE_BASE + shiftId,
            builder.setSmallIcon(context.applicationInfo.icon)
                .setContentTitle("做完记 班次提醒")
                .setContentText("班次开始：$shiftName")
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()
        )
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
