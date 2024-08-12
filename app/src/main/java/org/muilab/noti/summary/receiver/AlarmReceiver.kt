package org.muilab.noti.summary.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import org.muilab.noti.summary.database.room.ScheduleDatabase
import org.muilab.noti.summary.model.Schedule
import org.muilab.noti.summary.util.TAG
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.

        Log.d(TAG, "onReceive")

        val refreshSummaryIntent = Intent("edu.mui.noti.summary.REQUEST_ALLNOTIS_SCHEDULED")
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            refreshSummaryIntent.apply {
                setPackage(context.packageName)
            }
        )

        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)
        if (hour != -1 && minute != -1)
            scheduleNextDayAlarm(context, hour, minute)
    }

    private fun scheduleNextDayAlarm(context: Context, hour: Int, minute: Int) {
        scope.launch {
            val schedule = getAlarmTimeFromDatabase(context, hour, minute) ?: return@launch

            val calendar = Calendar.getInstance().apply {
                timeZone = TimeZone.getDefault()
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                add(Calendar.DAY_OF_YEAR, 1)
            }

            if (!schedule.isEveryDay()) {
                val weekRepeating = schedule.calendarWeek()

                while (!weekRepeating.contains(calendar.get(Calendar.DAY_OF_WEEK))) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            Log.d("scheduleNextDayAlarm", "${calendar.time}")

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("hour", schedule.hour)
            intent.putExtra("minute", schedule.minute)
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private suspend fun getAlarmTimeFromDatabase(context: Context, hour: Int, minute: Int): Schedule? {
        val scheduleDatabase by lazy { ScheduleDatabase.getInstance(context) }
        val scheduleDao = scheduleDatabase.scheduleDao()

        return withContext(Dispatchers.IO) {
            scheduleDao.getScheduleByTime(hour, minute)
        }
    }
}