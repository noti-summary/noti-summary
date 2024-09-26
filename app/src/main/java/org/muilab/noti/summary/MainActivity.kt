package org.muilab.noti.summary

import android.app.ActivityManager
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.muilab.noti.summary.database.room.APIKeyDatabase
import org.muilab.noti.summary.database.room.PromptDatabase
import org.muilab.noti.summary.database.room.ScheduleDatabase
import org.muilab.noti.summary.service.NotiListenerService
import org.muilab.noti.summary.service.SummaryService
import org.muilab.noti.summary.ui.theme.NotiappTheme
import org.muilab.noti.summary.util.TAG
import org.muilab.noti.summary.view.MainScreenView
import org.muilab.noti.summary.view.settings.APICreationLink
import org.muilab.noti.summary.view.settings.APIKeyEditor
import org.muilab.noti.summary.view.userInit.AskPermissionDialog
import org.muilab.noti.summary.view.userInit.FilterNotify
import org.muilab.noti.summary.viewModel.APIKeyViewModel
import org.muilab.noti.summary.viewModel.PromptViewModel
import org.muilab.noti.summary.viewModel.ScheduleViewModel
import org.muilab.noti.summary.viewModel.SummaryViewModel


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notiListenerIntent = Intent(this@MainActivity, NotiListenerService::class.java)

        val summaryServiceIntent = Intent(this, SummaryService::class.java)
        if (!isServiceRunning(SummaryService::class.java)) {
            Log.d("SummaryService", "Start service")
            startService(
                summaryServiceIntent.apply {
                    setPackage(packageName)
                }
            )
        }
        bindService(summaryServiceIntent, summaryServiceConnection, Context.BIND_AUTO_CREATE)

        val sharedPref = this.getSharedPreferences("user", Context.MODE_PRIVATE)
        setContent {

            var initStatus by remember {
                mutableStateOf(sharedPref.getString("initStatus", "NOT_STARTED").toString())
            }

            NotiappTheme {
                when (initStatus) {
                    "NOT_STARTED" -> {
                        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                            val intent = Intent().apply {
                                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            }
                            startActivity(intent)
                        }
                        AskPermissionDialog (
                            onAgree = {
                                if (isNotiListenerEnabled()) {
                                    with(sharedPref.edit()) {
                                        putString("initStatus", "ALLOW_EXACT_ALARM")
                                        apply()
                                    }
                                    initStatus = "ALLOW_EXACT_ALARM"
                                } else {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.permission_not_yet_enabled),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            OpenPermission = {
                                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                    }
                    "ALLOW_EXACT_ALARM" -> {
                        Log.d(TAG, "ALLOW_EXACT_ALARM")
                        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        if (!alarmManager.canScheduleExactAlarms()) {
                            startActivity(Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        } else {
                            with(sharedPref.edit()) {
                                putString("initStatus", "SHOW_FILTER_NOTICE")
                                apply()
                            }
                            initStatus = "SHOW_FILTER_NOTICE"
                        }
                    }
                    "SHOW_FILTER_NOTICE" -> {
                        Log.d(TAG, "SHOW_FILTER_NOTICE")
                        if (!isNotiListenerEnabled())
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        FilterNotify(onAgree = {
                            with(sharedPref.edit()) {
                                putString("initStatus", "USER_READY")
                                apply()
                            }
                            initStatus = "USER_READY"
                        })
                    }
                    "USER_READY" -> {
                        Log.d(TAG, "USER_READY")
                        val showDialog = remember { mutableStateOf(true) }
                        val selectedOption = apiViewModel.apiKey.value
                        if (selectedOption?.startsWith("sk-") == true) {
                            with(sharedPref.edit()) {
                                putString("initStatus", "USER_PROVIDED_KEY")
                                apply()
                            }
                            initStatus = "USER_PROVIDED_KEY"
                        } else {

                            LaunchedEffect(true) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val allAPIKey = apiKeyDatabase.apiKeyDao().getAllAPIStatic()
                                    if (allAPIKey.isNotEmpty()) {
                                        showDialog.value = false
                                        apiViewModel.chooseAPI(allAPIKey[0])
                                        with(sharedPref.edit()) {
                                            putString("initStatus", "USER_PROVIDED_KEY")
                                            apply()
                                        }
                                        initStatus = "USER_PROVIDED_KEY"
                                    }
                                }
                            }

                            val inputKey = remember { mutableStateOf("") }
                            val titleContent: @Composable () -> Unit = {
                                Column {
                                    Image(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 30.dp, bottom = 20.dp)
                                            .height(70.dp),
                                        painter = painterResource(id = R.drawable.key),
                                        contentDescription = "key_icon",
                                    )
                                    Text(
                                        text = stringResource(R.string.key_requirement),
                                        modifier = Modifier.padding(15.dp, 0.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    APICreationLink()
                                }
                            }
                            val confirmAction = {
                                if (inputKey.value != "" && inputKey.value.startsWith("sk-")) {
                                    apiViewModel.addAPI(inputKey.value)
                                    inputKey.value = ""
                                    showDialog.value = false
                                    with(sharedPref.edit()) {
                                        putString("initStatus", "USER_PROVIDED_KEY")
                                        apply()
                                    }
                                    initStatus = "USER_PROVIDED_KEY"
                                }
                            }
                            if (showDialog.value)
                                APIKeyEditor(showDialog, inputKey, titleContent, confirmAction)
                        }
                    }
                    "USER_PROVIDED_KEY" -> {
                        startService(
                            notiListenerIntent.apply {
                                setPackage(packageName)
                            }
                        )
                        MainScreenView(
                            this,
                            sumViewModel,
                            promptViewModel,
                            apiViewModel,
                            scheduleViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPref = this.getSharedPreferences("user", Context.MODE_PRIVATE)
        val stage = sharedPref.getString("initStatus", "NOT_STARTED")
        if (stage.equals("USER_READY") && !isNotiListenerEnabled())
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

        val allNotiFilter = IntentFilter("edu.mui.noti.summary.RETURN_ALLNOTIS")
        registerReceiver(allNotiReturnReceiver, allNotiFilter, RECEIVER_NOT_EXPORTED)
        val newStatusFilter = IntentFilter("edu.mui.noti.summary.UPDATE_STATUS")
        registerReceiver(newStatusReceiver, newStatusFilter, RECEIVER_NOT_EXPORTED)
        sumViewModel.updateNotiDrawer()
        sumViewModel.updateStatus()
    }

    override fun onPause() {
        unregisterReceiver(allNotiReturnReceiver)
        unregisterReceiver(newStatusReceiver)
        super.onPause()
    }

    override fun onDestroy() {
        unbindService(summaryServiceConnection)
        super.onDestroy()
    }

    private lateinit var summaryService: SummaryService

    private val sumViewModel by viewModels<SummaryViewModel>()

    private val promptDatabase by lazy { PromptDatabase.getInstance(this) }
    private val promptViewModel by lazy { PromptViewModel(application, promptDatabase) }

    private val apiKeyDatabase by lazy { APIKeyDatabase.getInstance(this) }
    private val apiViewModel by lazy { APIKeyViewModel(application, apiKeyDatabase) }

    private val scheduleDatabase by lazy { ScheduleDatabase.getInstance(this) }
    private val scheduleViewModel by lazy { ScheduleViewModel(application, scheduleDatabase) }

    private fun isNotiListenerEnabled(): Boolean {
        val cn = ComponentName(this, NotiListenerService::class.java)
        val flat: String? =
            Settings.Secure.getString(this.contentResolver, "enabled_notification_listeners")
        return (flat != null) && (cn.flattenToString() in flat)
    }

    private val summaryServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d("SummaryService", "Bind service")
            val binder = service as SummaryService.SummaryBinder
            summaryService = binder.getService()
            sumViewModel.setService(summaryService)
            Log.d("SummaryService", "Bind service")
        }

        override fun onServiceDisconnected(className: ComponentName) {

        }
    }

    private val allNotiReturnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "edu.mui.noti.summary.RETURN_ALLNOTIS") {
                val activeKeys = intent.getSerializableExtra("activeKeys") as? ArrayList<Pair<String, String>>
                if (activeKeys != null) {
                    sumViewModel.updateSummaryText(activeKeys, false)
                }
            }
        }
    }

    private val newStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "edu.mui.noti.summary.UPDATE_STATUS") {
                sumViewModel.updateStatus()
            }
        }
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE))
            if (serviceClass.name == service.service.className)
                return true
        return false
    }
}
