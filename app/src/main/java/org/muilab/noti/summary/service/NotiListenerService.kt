package org.muilab.noti.summary.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.noti.summary.database.room.DrawerDatabase
import org.muilab.noti.summary.model.NotiUnit
import org.muilab.noti.summary.util.TAG
import org.muilab.noti.summary.util.getAppFilter
import org.muilab.noti.summary.util.getDatabaseNotifications
import org.muilab.noti.summary.util.getNotiDrawer

class NotiListenerService: NotificationListenerService() {

    private var connected: Boolean = false
    private var intentRegistered: Boolean = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        CoroutineScope(Dispatchers.IO).launch {
            val activeKeys = getActiveKeys()
            val databaseNotifications = getDatabaseNotifications(applicationContext, activeKeys)
            val appFilter = getAppFilter(applicationContext)
            getNotiDrawer(applicationContext, databaseNotifications, appFilter)
        }
        val updateIntent = Intent("edu.mui.noti.summary.UPDATE_STATUS")
        sendBroadcast(
            updateIntent.apply {
                setPackage(packageName)
            }
        )
        Log.i(TAG, "Connected!")
        connected = true
    }

    override fun onListenerDisconnected() {
        connected = false
        super.onListenerDisconnected()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    private val allNotiRequestReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "edu.mui.noti.summary.REQUEST_ALLNOTIS") {
                val broadcastIntent = Intent("edu.mui.noti.summary.RETURN_ALLNOTIS")
                broadcastIntent.putExtra("activeKeys", getActiveKeys())
                sendBroadcast(
                    broadcastIntent.apply {
                        setPackage(packageName)
                    }
                )
            }
        }
    }

    private val allNotiRequestScheduledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "edu.mui.noti.summary.REQUEST_ALLNOTIS_SCHEDULED") {
                val broadcastIntent = Intent("edu.mui.noti.summary.RETURN_ALLNOTIS_SCHEDULED")
                broadcastIntent.putExtra("activeKeys", getActiveKeys())
                sendBroadcast(
                    broadcastIntent.apply {
                        setPackage(packageName)
                    }
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        val allNotiFilter = IntentFilter("edu.mui.noti.summary.REQUEST_ALLNOTIS")
        val allNotiFilterScheduled = IntentFilter("edu.mui.noti.summary.REQUEST_ALLNOTIS_SCHEDULED")
        LocalBroadcastManager.getInstance(this).registerReceiver(allNotiRequestReceiver, allNotiFilter)
        LocalBroadcastManager.getInstance(this).registerReceiver(allNotiRequestScheduledReceiver, allNotiFilterScheduled)
        intentRegistered = true
    }

    override fun onDestroy() {
        val restartServiceIntent = Intent(applicationContext, NotiListenerService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT)
        getSystemService(Context.ALARM_SERVICE)
        val alarmService: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis() + 10000, restartServicePendingIntent)
        if (intentRegistered) {
            try {
                unregisterReceiver(allNotiRequestReceiver)
                unregisterReceiver(allNotiRequestScheduledReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                intentRegistered = false
            }
        }
        Log.d(TAG, "onDestroy")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (connected) {
            CoroutineScope(Dispatchers.IO).launch {
                val activeKeys = getActiveKeys()
                val databaseNotifications = getDatabaseNotifications(applicationContext, activeKeys)
                val appFilter = getAppFilter(applicationContext)
                getNotiDrawer(applicationContext, databaseNotifications, appFilter)
            }
        }
        return START_STICKY
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!sbn.isOngoing && sbn.packageName != packageName) {
            insertNoti(sbn)
            CoroutineScope(Dispatchers.IO).launch {
                val activeKeys = getActiveKeys()
                val databaseNotifications = getDatabaseNotifications(applicationContext, activeKeys)
                val appFilter = getAppFilter(applicationContext)
                getNotiDrawer(applicationContext, databaseNotifications, appFilter)
            }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap?,
        reason: Int
    ) {
        fun getNotificationReasonString(reason: Int): String {
            return when(reason) {
                REASON_APP_CANCEL -> "REASON_APP_CANCEL"
                REASON_APP_CANCEL_ALL -> "REASON_APP_CANCEL_ALL"
                REASON_ASSISTANT_CANCEL -> "REASON_ASSISTANT_CANCEL"
                REASON_CANCEL -> "REASON_CANCEL"
                REASON_CANCEL_ALL -> "REASON_CANCEL_ALL"
                REASON_CHANNEL_BANNED -> "REASON_CHANNEL_BANNED"
                REASON_CHANNEL_REMOVED -> "REASON_CHANNEL_REMOVED"
                REASON_CLEAR_DATA -> "REASON_CLEAR_DATA"
                REASON_CLICK -> "REASON_CLICK"
                REASON_ERROR -> "REASON_ERROR"
                REASON_GROUP_OPTIMIZATION -> "REASON_GROUP_OPTIMIZATION"
                REASON_GROUP_SUMMARY_CANCELED -> "REASON_GROUP_SUMMARY_CANCELED"
                REASON_LISTENER_CANCEL -> "REASON_LISTENER_CANCEL"
                REASON_LISTENER_CANCEL_ALL -> "REASON_LISTENER_CANCEL_ALL"
                REASON_PACKAGE_BANNED -> "REASON_PACKAGE_BANNED"
                REASON_PACKAGE_CHANGED -> "REASON_PACKAGE_CHANGED"
                REASON_PACKAGE_SUSPENDED -> "REASON_PACKAGE_SUSPENDED"
                REASON_PROFILE_TURNED_OFF -> "REASON_PROFILE_TURNED_OFF"
                REASON_SNOOZED -> "REASON_SNOOZED"
                REASON_TIMEOUT -> "REASON_TIMEOUT"
                REASON_UNAUTOBUNDLED -> "REASON_UNAUTOBUNDLED"
                REASON_USER_STOPPED -> "REASON_USER_STOPPED"
                else -> "UNKNOWN_REASON"
            }
        }

        if (sbn.isOngoing || sbn.packageName == packageName)
            return

        val notiUnit = NotiUnit(applicationContext, sbn)
        val reasonStr = getNotificationReasonString(reason)

        CoroutineScope(Dispatchers.IO).launch {
            val drawerDatabase = DrawerDatabase.getInstance(applicationContext)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteByPackageSortKey(
                notiUnit.pkgName,
                notiUnit.sbnKey,
                notiUnit.groupKey,
                notiUnit.sortKey
            )
        }

        val notiKey = notiUnit.sbnKey
        val summarySharedPref = getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)
        val prevRemovedNotisJson = summarySharedPref.getString("removedNotis", "{}")
        val removedNotisType = object : TypeToken<MutableMap<String, String>>() {}.type
        val removedNotis = Gson().fromJson<MutableMap<String, String>>(prevRemovedNotisJson, removedNotisType)
        if (notiKey !in removedNotis.keys)
            removedNotis[notiKey] = "${reasonStr}_${System.currentTimeMillis()}"
        val newRemovedNotisJson = Gson().toJson(removedNotis)
        summarySharedPref.edit().putString("removedNotis", newRemovedNotisJson).apply()

        CoroutineScope(Dispatchers.IO).launch {
            val activeKeys = getActiveKeys()
            val databaseNotifications = getDatabaseNotifications(applicationContext, activeKeys)
            val appFilter = getAppFilter(applicationContext)
            getNotiDrawer(applicationContext, databaseNotifications, appFilter)
        }
    }

    private fun insertNoti(sbn: StatusBarNotification) {
        val notiUnit = NotiUnit(applicationContext, sbn)
        if (notiUnit.title == "null" || notiUnit.content == "null")
            return
        CoroutineScope(Dispatchers.IO).launch {
            val drawerDatabase = DrawerDatabase.getInstance(applicationContext)
            val drawerDao = drawerDatabase.drawerDao()
            drawerDao.deleteByVisibleAttr(
                notiUnit.pkgName,
                notiUnit.`when`,
                notiUnit.title,
                notiUnit.content
            )
            if (notiUnit.sortKey != "null")
                drawerDao.deleteByPackageSortKey(
                    notiUnit.pkgName,
                    notiUnit.sbnKey,
                    notiUnit.groupKey,
                    notiUnit.sortKey
                )
            drawerDao.insert(notiUnit)
        }
    }

    fun getActiveKeys(): ArrayList<Pair<String, String>> {

        activeNotifications.forEach { sbn ->
            if (!sbn.isOngoing)
                insertNoti(sbn)
        }
        val sbnKeys = activeNotifications
            .map {
                val notiUnit = NotiUnit(applicationContext, it)
                notiUnit.pkgName to notiUnit.sbnKey
            }
            .distinct()
            .toCollection(ArrayList())

        val activeKeysJson = Gson().toJson(sbnKeys)
        val summaryPref = getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)
        with (summaryPref.edit()) {
            putString("activeKeys", activeKeysJson)
            apply()
        }

        return sbnKeys
    }

}