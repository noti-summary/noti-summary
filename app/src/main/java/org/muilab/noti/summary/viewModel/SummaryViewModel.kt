package org.muilab.noti.summary.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.muilab.noti.summary.model.NotiUnit
import org.muilab.noti.summary.service.SummaryService
import org.muilab.noti.summary.util.TAG
import org.muilab.noti.summary.util.getAppFilter
import org.muilab.noti.summary.util.getDatabaseNotifications
import org.muilab.noti.summary.util.getNotiDrawer
import org.muilab.noti.summary.view.home.SummaryResponse
import kotlin.coroutines.cancellation.CancellationException

class SummaryViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPreferences =
        getApplication<Application>().getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)

    private val _notifications = MutableLiveData<List<NotiUnit>>()
    val notifications: LiveData<List<NotiUnit>> = _notifications

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext

    @SuppressLint("StaticFieldLeak")
    private lateinit var summaryService: SummaryService

    init {
        updateFromSharedPref()
        resetNotiDrawer()
    }

    private fun updateFromSharedPref() {
        val resultValue = sharedPreferences.getString(
            "resultValue", context.getString(SummaryResponse.HINT.message))
        _result.value = resultValue!!
    }

    fun setService(summaryService: SummaryService) {
        this.summaryService = summaryService
        if (summaryService.getStatusText() != context.getString(SummaryResponse.GENERATING.message)) {
            updateFromSharedPref()
        } else
            updateStatus()
    }

    fun resetNotiDrawer() {
        val json = sharedPreferences.getString("notiDrawer", "").toString()
        if (json.isEmpty())
            _notifications.value = listOf()
        else {
            val notiDrawerType = object : TypeToken<ArrayList<NotiUnit>>() {}.type
            val notiDrawer = Gson().fromJson<ArrayList<NotiUnit>>(json, notiDrawerType)
            _notifications.value = notiDrawer
        }
    }

    fun updateNotiDrawer() {
        val activeKeyJson = sharedPreferences.getString("activeKeys", "").toString()
        if (activeKeyJson.isNotEmpty()) {
            val activeKeyType = object : TypeToken<ArrayList<Pair<String, String>>>() {}.type
            val activeKeys =
                Gson().fromJson<ArrayList<Pair<String, String>>>(activeKeyJson, activeKeyType)
            CoroutineScope(Dispatchers.IO).launch {
                val databaseNotifications = getDatabaseNotifications(context, activeKeys)
                val appFilter = getAppFilter(context)
                getNotiDrawer(context, databaseNotifications, appFilter)
            }
            resetNotiDrawer()
        }
    }

    fun getSummaryText() {
        val intent = Intent("edu.mui.noti.summary.REQUEST_ALLNOTIS")
        Log.d(TAG, "getSummaryText");
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            intent.apply {
                setPackage(context.packageName)
            }
        )
    }

    fun updateStatus() {
        if (!::summaryService.isInitialized)
            return

        val newStatus = summaryService.getStatusText()
        if (newStatus.isNotEmpty())
            _result.postValue(newStatus)
        updateNotiDrawer()
    }

    fun updateSummaryText(activeKeys: ArrayList<Pair<String, String>>, isScheduled: Boolean) {
        _result.postValue(context.getString(SummaryResponse.GENERATING.message))
        viewModelScope.launch {
            try {
                val timeoutThreshold = 600000L
                withTimeout(timeoutThreshold) {
                    val responseMessage = summaryService.sendToServer(activeKeys, isScheduled)
                    _result.postValue(responseMessage)
                }
            } catch (e: TimeoutCancellationException) {
                _result.postValue(context.getString(SummaryResponse.TIMEOUT_ERROR.message))
            } catch (e: java.util.concurrent.CancellationException) {
                _result.postValue(context.getString(SummaryResponse.TIMEOUT_ERROR.message))
            } catch (e: CancellationException) {
                _result.postValue(context.getString(SummaryResponse.TIMEOUT_ERROR.message))
            } catch (e: Exception) {
                _result.postValue(context.getString(SummaryResponse.UNKNOWN_ERROR.message))
            }
            resetNotiDrawer()
        }
    }
}