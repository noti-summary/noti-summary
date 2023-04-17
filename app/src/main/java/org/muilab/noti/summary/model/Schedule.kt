package org.muilab.noti.summary.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_events")
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    var primaryKey: Int = 0,
    var hour: Int,
    var minute: Int,
    var week: Int = 0b1111111
    // 7-bit binary number for the week, with each bit representing a day from Sunday to Saturday.
) {
    fun getTime(): String {
        return String.format("%02d:%02d", hour, minute)
    }

    fun getWeekString(): String {
        return when (week) {
            0b1111111 -> "every day"
            0b1000001 -> "every week"
            else -> {
                val days = mutableListOf<String>()
                val weekdays = listOf("Mon.", "Tue.", "Wed.", "Thu.", "Fri.", "Sat.", "Sun.")
                for (i in weekdays.indices) {
                    if ((week and (1 shl i)) != 0) {
                        days.add(weekdays[i])
                    }
                }
                if (days.size == 1) "every ${days[0]}" else days.joinToString(" ")
            }
        }
    }
}
