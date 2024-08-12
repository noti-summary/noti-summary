package org.muilab.noti.summary.view.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.muilab.noti.summary.R
import org.muilab.noti.summary.model.Schedule
import org.muilab.noti.summary.util.*
import org.muilab.noti.summary.viewModel.ScheduleViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*

@Composable
fun SchedulerScreen(context: Context, scheduleViewModel: ScheduleViewModel) {
    val sharedPref = context.getSharedPreferences("noti-send", Context.MODE_PRIVATE)
    var checkedState by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            with(sharedPref.edit()) {
                putBoolean("send_or_not", false)
                apply()
            }
        }
        checkedState = sharedPref.getBoolean("send_or_not", false)
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.push_notification),
                modifier = Modifier.padding(horizontal = 30.dp).weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                modifier = Modifier.padding(horizontal = 30.dp),
                checked = checkedState,
                onCheckedChange = { newState ->
                    if (newState && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        showDialog = true
                    } else {
                        checkedState = newState
                        with(sharedPref.edit()) {
                            putBoolean("send_or_not", newState)
                            apply()
                        }
                    }
                }
            )
        }
        TimeList(context, scheduleViewModel)
    }

    if (showDialog) {
        AlertDialog(
            title = { Text(stringResource(R.string.no_noti_permission)) },
            text = { Text(stringResource(R.string.click_to_open_permission)) },
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        showDialog = false
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            setPackage(context.packageName)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.ok),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 30.dp, end = 30.dp)
                    )
                }
            },
        )
    }

    AddScheduleButton(context, scheduleViewModel)
}

@Composable
fun TimeList(context: Context, scheduleViewModel: ScheduleViewModel) {
    val allSchedule = scheduleViewModel.allSchedule.observeAsState(listOf())
    var showDialog by remember { mutableStateOf(false) }
    var editSchedule by remember { mutableStateOf(Schedule(0, -1, -1, -1)) }

    Column {
        LazyColumn(modifier = Modifier.fillMaxHeight()) {
            itemsIndexed(allSchedule.value) { index, item ->
                if (index == 0) Divider(color = Color.DarkGray, thickness = 1.dp)
                Box(
                    modifier = Modifier
                        .padding(start = 15.dp, end = 15.dp, top = 2.dp, bottom = 2.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) {
                            Text(
                                text = item.getTime(),
                                style = MaterialTheme.typography.displaySmall
                            )
                            Text(
                                text = item.getWeekString(context),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        IconButton(onClick = {
                            editSchedule = item
                            showDialog = true
                        }) {
                            Icon(
                                modifier = Modifier.padding(all = 11.dp).size(20.dp),
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "weekly schedule"
                            )
                        }
                        IconButton(onClick = {
                            scheduleViewModel.deleteSchedule(item)
                            deleteAlarm(context, item)
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "delete schedule")
                        }
                    }
                }
                Divider(color = Color.DarkGray, thickness = 1.dp)
            }
        }
    }

    val coroutineScope = CoroutineScope(Dispatchers.IO)
    if (showDialog) {
        val weekState = remember { mutableStateOf(editSchedule.week) }
        val confirmAction = {
            showDialog = !showDialog
            coroutineScope.launch {
                scheduleViewModel.updateWeekSchedule(editSchedule, weekState.value)
                editSchedule.week = weekState.value
                updateAlarm(context, editSchedule)
            }
        }

        SetDayOfWeekDialog(weekState, onDismissRequest = { showDialog = !showDialog }, confirmAction)
    }
}

@Composable
fun SetDayOfWeekDialog(
    weekState: MutableState<Int>,
    onDismissRequest: () -> Unit,
    confirmAction: () -> Job
) {
    val daysOfWeek = DayOfWeek.values().map {
        it.getDisplayName(TextStyle.FULL, Locale.getDefault())
    }

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            modifier = Modifier.width(screenWidth * 4 / 5).height(screenHeight * 1 / 2)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        itemsIndexed(daysOfWeek) { idx, week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(
                                            topStart = if (idx == 0) 8.dp else 0.dp,
                                            topEnd = if (idx == 0) 8.dp else 0.dp,
                                            bottomStart = if (idx == 6) 8.dp else 0.dp,
                                            bottomEnd = if (idx == 6) 8.dp else 0.dp
                                        )
                                    )
                                    .clickable {
                                        weekState.value = weekState.value xor (1 shl (6 - idx))
                                    },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    modifier = Modifier.padding(12.dp).weight(1f),
                                    text = week,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                if (weekState.value and (1 shl (6 - idx)) != 0) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(modifier = Modifier.padding(8.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                    onClick = { confirmAction() }
                ) {
                    Text(text = stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
fun AddScheduleButton(context: Context, scheduleViewModel: ScheduleViewModel) {
    val listener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            val newSchedule = scheduleViewModel.addNewSchedule(hour, minute)
            if (newSchedule != null) {
                addAlarm(context, newSchedule)
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp, end = 20.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(onClick = {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val picker = TimePickerDialog(context, listener, 0, 0, false)
            picker.show()
            picker.updateTime(hour, minute)
        }) {
            Icon(Icons.Filled.Add, "schedule summary")
        }
    }
}