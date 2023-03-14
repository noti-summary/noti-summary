package org.muilab.noti.summary.viewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import org.muilab.noti.summary.util.TAG
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONException
import org.muilab.noti.summary.model.UserCredit
import java.util.concurrent.TimeUnit

class SummaryViewModel(application: Application): AndroidViewModel(application) {
    private val sharedPreferences = getApplication<Application>().getSharedPreferences("SummaryPref", Context.MODE_PRIVATE)
    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result
    @SuppressLint("StaticFieldLeak")
    private val context = getApplication<Application>().applicationContext
    // private val prompt = "你將摘要手機通知，請從以下通知(段落的先後順序不一定和重要性相關)篩選出重要資訊，並以一段文字說明，不要自行臆測、延伸與解釋"
    private val prompt = "The following is a list of notifications given in random order. Please pick and summarize important information into a paragraph using Traditional Chinese, with the most urgent information mentioned first."

    private val dotenv = dotenv {
        directory = "./assets"
        filename = "env"
    }
    private val serverIP = dotenv["SUMMARY_URL"]
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    init {
        val resultValue = sharedPreferences.getString("resultValue", "")
        if (resultValue != "") {
            _result.value = resultValue
        }
    }

    private fun updateLiveDataValue(newValue: String?) {
        if (newValue != null) {
            _result.postValue(newValue)
            val editor = sharedPreferences.edit()
            editor.putString("resultValue", newValue)
            editor.apply()

            subtractCredit(1)
        }
    }

    fun getSummaryText() {
        val intent = Intent("edu.mui.noti.summary.REQUEST_ALLNOTIS")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun updateSummaryText(postContent: String) {
        _result.postValue("通知摘要產生中，請稍候...")
        viewModelScope.launch {
            sendToServer(postContent)
        }
    }

    private suspend fun sendToServer(content: String) = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val formattedContent = content.replace("\n", "\\n").replace("\"", "\'")
        val postBody = "{\"prompt\": \"$prompt\", \"content\": \"${formattedContent}\"}"
        val request = Request.Builder()
            .url(serverIP)
            .post(postBody.toRequestBody(mediaType))
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val summary = response.body?.string()?.replace("\\n", "\r\n")?.removeSurrounding("\"")
                updateLiveDataValue(summary)
            } else {
                response.body?.let { Log.i("Server", it.string()) }
            }
        } catch (e: IOException) {
            _result.postValue("無法連線...請確認網路連線！")
        } catch (e: JSONException) {
            _result.postValue(e.toString())
        }
    }

    private fun subtractCredit(number: Int) {
        val sharedPref = context.getSharedPreferences("user_id", Context.MODE_PRIVATE)
        val userId = sharedPref.getString("user_id", "000").toString()

        val db = Firebase.firestore
        val docRef = db.collection("user-free-credit").document(userId)
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val res = document.toObject<UserCredit>()!!
                docRef.update("credit", res.credit-number)
            }
        }
    }
}