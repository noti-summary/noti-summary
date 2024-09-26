package org.muilab.noti.summary.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.AuthenticationException
import com.aallam.openai.api.exception.OpenAIAPIException
import com.aallam.openai.api.exception.OpenAIHttpException
import com.aallam.openai.api.exception.RateLimitException
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.zqc.opencc.android.lib.ChineseConverter
import com.zqc.opencc.android.lib.ConversionType
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.muilab.noti.summary.MainActivity
import org.muilab.noti.summary.R
import org.muilab.noti.summary.model.NotiUnit
import org.muilab.noti.summary.util.TAG
import org.muilab.noti.summary.util.getAppFilter
import org.muilab.noti.summary.util.getDatabaseNotifications
import org.muilab.noti.summary.util.getNotiDrawer
import org.muilab.noti.summary.util.isNetworkConnected
import org.muilab.noti.summary.view.home.SummaryResponse
import java.util.LinkedList
import java.util.Queue
import kotlin.collections.set
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes


class SummaryService : Service(), LifecycleOwner {

    private lateinit var lifecycleRegistry: LifecycleRegistry

    // Binder given to clients
    private val binder = SummaryBinder()

    private lateinit var summaryPref: SharedPreferences
    private lateinit var promptPref: SharedPreferences
    private lateinit var apiPref: SharedPreferences
    private var NOTI_THRESHOLD = 0
    private var SUMMARY_THRESHOLD = 0
    private var statusText = ""

    private fun getPostContent(activeNotifications: ArrayList<NotiUnit>): ArrayList<String> {

        val contentList = arrayListOf<String>()

        val sb = StringBuilder()
        activeNotifications.forEach { noti ->

            val filterMap = mutableMapOf<String, String>()
            filterMap[getString(R.string.application_name)] = "App: ${noti.appName}"
            filterMap[getString(R.string.time)] = "Time: ${noti.time}"
            filterMap[getString(R.string.title)] = "Title: ${noti.title}"
            filterMap[getString(R.string.content)] = "Content: ${noti.content}"

            val notiFilterPrefs = getSharedPreferences("noti_filter", Context.MODE_PRIVATE)
            val input = filterMap.filter { attribute -> notiFilterPrefs.getBoolean(attribute.key, true) }
                    .values
                    .joinToString(separator = ", ")

            if (sb.length + input.length > NOTI_THRESHOLD) {
                contentList.add(sb.toString())
                sb.clear()
            }
            sb.append("$input\n")
        }
        if (sb.isNotEmpty())
            contentList.add(sb.toString())
        return contentList
    }

    suspend fun sendToServer(
        activeKeys: ArrayList<Pair<String, String>>,
        isScheduled: Boolean
    ): String {
        return withContext(Dispatchers.IO) {

            val databaseNotifications = getDatabaseNotifications(applicationContext, activeKeys)
            val appFilterMap = getAppFilter(applicationContext)
            val summarizedNotifications = getNotiDrawer(
                applicationContext,
                databaseNotifications,
                appFilterMap
            )

            updateStatusText(getString(SummaryResponse.GENERATING.message))

            var responseStr = ""
            val userAPIKey = apiPref.getString("userAPIKey", getString(R.string.key_not_provided))!!
            val openAI = run {
                val config = OpenAIConfig(
                    userAPIKey,
                    timeout = Timeout(15.minutes, 20.minutes, 25.minutes)
                )
                OpenAI(config)
            }
            val modelChoice = summaryPref.getBoolean("model", false)
            val model = if (modelChoice) "gpt-4" else "gpt-3.5-turbo-16k"
            NOTI_THRESHOLD = if (modelChoice) 8000 else 16000
            SUMMARY_THRESHOLD = if (modelChoice) 12000 else 24000

            if (!isNetworkConnected(applicationContext))
                responseStr = getString(SummaryResponse.NETWORK_ERROR.message)
            else if (summarizedNotifications.isNotEmpty() && userAPIKey != getString(R.string.key_not_provided)) {

                val postContent = getPostContent(summarizedNotifications)
                postContent.forEach{ Log.d("len", "${it.length}") }
                val prompt = promptPref.getString(
                    "curPrompt",
                    getString(R.string.default_summary_prompt)
                ) as String

                Log.d(TAG, userAPIKey)
                val subSummaries = mutableListOf<String>()
                for (chunk in postContent) {
                    val chatCompletionRequest = ChatCompletionRequest(
                        model = ModelId(model),
                        messages = listOf(
                            ChatMessage(
                                role = ChatRole.System,
                                content = getString(R.string.prompt_opening)
                            ),
                            ChatMessage(
                                role = ChatRole.User,
                                content = chunk
                            ),
                            ChatMessage(
                                role = ChatRole.System,
                                content = prompt
                            )
                        )
                    )
                    try {
                        val response = openAI.chatCompletion(chatCompletionRequest)
                        val message = response.choices.first().message
                        val finishReason = response.choices.first().finishReason
                        Log.d(TAG, "${message.content} $finishReason")
                        val responseText = message.content?.replace("\\n", "\r\n")?.replace("\\", "")
                            ?.removeSurrounding("\"")
                        val subSummary = ChineseConverter.convert(
                            responseText,
                            ConversionType.S2TWP,
                            applicationContext
                        )
                        subSummaries.add(subSummary)
                    } catch (e: OpenAIAPIException) {
                        Log.d("Error", e.stackTraceToString())
                        responseStr = when (e.statusCode) {
                            401 -> getString(SummaryResponse.APIKEY_ERROR.message)
                            402 -> getString(SummaryResponse.APIKEY_ERROR.message)
                            429 -> getString(SummaryResponse.QUOTA_ERROR.message)
                            500 -> getString(SummaryResponse.SERVER_ERROR.message)
                            503 -> getString(SummaryResponse.TIMEOUT_ERROR.message)
                            else -> getString(SummaryResponse.UNKNOWN_ERROR.message)
                        }
                        break
                    } catch (e: AuthenticationException) {
                        Log.d("Error", e.stackTraceToString())
                        responseStr = getString(SummaryResponse.APIKEY_ERROR.message)
                        break
                    } catch (e: RateLimitException) {
                        Log.d("Error", e.stackTraceToString())
                        responseStr = getString(SummaryResponse.TIMEOUT_ERROR.message)
                        break
                    } catch (e: Exception) {
                        Log.d("Error", e.stackTraceToString())
                        responseStr = getString(SummaryResponse.UNKNOWN_ERROR.message)
                    }
                }
                if (subSummaries.size == 1)
                    responseStr = subSummaries[0]


                else if (subSummaries.isNotEmpty()) {

                    val queue: Queue<String> = LinkedList(subSummaries)
                    while (queue.size > 1) {
                        val sb = StringBuilder()
                        while (queue.isNotEmpty() && (sb.length + queue.peek()?.length!! <= SUMMARY_THRESHOLD)) {
                            Log.d("len", "${queue.peek()?.length}")
                            sb.append("${queue.peek()}\n\n")
                            queue.remove()
                        }

                        val chatCompletionRequest = ChatCompletionRequest(
                            model = ModelId(model),
                            messages = listOf(
                                ChatMessage(
                                    role = ChatRole.User,
                                    content = sb.toString()
                                ),
                                ChatMessage(
                                    role = ChatRole.System,
                                    content = "${getString(R.string.prompt_subsummary1)}$prompt${getString(R.string.prompt_subsummary2)}"
                                )
                            )
                        )
                        val response = openAI.chatCompletion(chatCompletionRequest)
                        val message = response.choices.first().message
                        queue.add(message.content)
                    }
                    responseStr = queue.peek()!!
                }
            } else {
                responseStr = getString(SummaryResponse.NO_NOTIFICATION.message)
            }
            updateStatusText(responseStr)
            if (isScheduled)
                notifySummary(responseStr)
            responseStr // return value
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        val allNotiFilterScheduled = IntentFilter("edu.mui.noti.summary.RETURN_ALLNOTIS_SCHEDULED")
        registerReceiver(allNotiReturnReceiver, allNotiFilterScheduled, RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        summaryPref = getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)
        promptPref = getSharedPreferences("PromptPref", Context.MODE_PRIVATE)
        apiPref = getSharedPreferences("ApiPref", Context.MODE_PRIVATE)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class SummaryBinder : Binder() {
        fun getService(): SummaryService {
            return this@SummaryService
        }
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        unregisterReceiver(allNotiReturnReceiver)
        super.onDestroy()
    }

    fun getStatusText(): String {
        return statusText
    }

    private fun updateStatusText(newStatus: String) {
        statusText = newStatus
        val updateIntent = Intent("edu.mui.noti.summary.UPDATE_STATUS")
        sendBroadcast(
            updateIntent.apply {
                setPackage(packageName)
            }
        )
    }

    private fun notifySummary(responseStr: String) {
        val sharedPref = getSharedPreferences("noti-send", Context.MODE_PRIVATE)
        val sendNotiOrNot = sharedPref.getBoolean("send_or_not", true)
        if (!sendNotiOrNot)
            return
        // If user don't want to send the notification, return directly

        val notiContentIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, notiContentIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(applicationContext, "Notify")
            .setSmallIcon(R.drawable.quotation)
            .setContentTitle(getString(R.string.noti_content))
            .setContentText(responseStr)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("Notify", "Notify", importance)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        val notificationManagerCompat = NotificationManagerCompat.from(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManagerCompat.notify(0, builder.build())
    }

    private val allNotiReturnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "edu.mui.noti.summary.RETURN_ALLNOTIS_SCHEDULED") {
                val activeKeys = intent.getSerializableExtra("activeKeys") as? ArrayList<Pair<String, String>>
                if (activeKeys != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val timeoutThreshold = 600000L
                            withTimeout(timeoutThreshold) {
                                sendToServer(activeKeys, true)
                            }
                        } catch (e: TimeoutCancellationException) {
                            updateStatusText(getString(SummaryResponse.TIMEOUT_ERROR.message))
                        } catch (e: java.util.concurrent.CancellationException) {
                            updateStatusText(getString(SummaryResponse.TIMEOUT_ERROR.message))
                        } catch (e: CancellationException) {
                            updateStatusText(getString(SummaryResponse.TIMEOUT_ERROR.message))
                        } catch (e: Exception) {
                            updateStatusText(getString(SummaryResponse.UNKNOWN_ERROR.message))
                        }
                    }
                }
            }
        }
    }
}