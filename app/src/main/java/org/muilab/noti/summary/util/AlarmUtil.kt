package org.muilab.noti.summary.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.Context
import android.content.Intent
import android.util.Log
import org.muilab.noti.summary.model.Schedule
import org.muilab.noti.summary.receiver.AlarmReceiver
import java.time.LocalTime
import java.util.*

fun addAlarm(context: Context, schedule: Schedule, isNotUpdate: Boolean = true) {
    Log.d("addAlarm", "time=${schedule.getTime()}")
    Log.d("primaryKey", schedule.primaryKey.toString())

    val calendar = Calendar.getInstance().apply {
        timeZone = TimeZone.getDefault()
        set(Calendar.HOUR_OF_DAY, schedule.hour)
        set(Calendar.MINUTE, schedule.minute)
        set(Calendar.SECOND, 0)
    }

    val now = LocalTime.now()
    val alarmTime = LocalTime.of(schedule.hour, schedule.minute)

    if (alarmTime < now)
        calendar.add(Calendar.DAY_OF_YEAR, 1)

    if (!schedule.isEveryDay()) {
        val weekRepeating = schedule.calendarWeek()

        while (!weekRepeating.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    intent.putExtra("hour", schedule.hour)
    intent.putExtra("minute", schedule.minute)

    val pendingIntent = PendingIntent.getBroadcast(
        context, schedule.primaryKey, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
    )

    alarmManager.setExactAndAllowWhileIdle(
        /* type = */ AlarmManager.RTC_WAKEUP,
        /* triggerAtMillis = */ calendar.timeInMillis,
        /* operation = */ pendingIntent
    )
}

fun updateAlarm(context: Context, schedule: Schedule) {
    deleteAlarm(context, schedule, isNotUpdate = false)
    addAlarm(context, schedule, isNotUpdate = false)
}

fun deleteAlarm(context: Context, schedule: Schedule, isNotUpdate: Boolean = true) {
    Log.d("deleteAlarm", "time=${schedule.getTime()}")
    Log.d("primaryKey", schedule.primaryKey.toString())

    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)
    intent.putExtra("hour", schedule.hour)
    intent.putExtra("minute", schedule.minute)
    val pendingIntent = PendingIntent.getBroadcast(
        context, schedule.primaryKey, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
    )

    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}