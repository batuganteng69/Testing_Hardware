package com.pjc.testinghardware

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val req = Get(this@MainActivity)
        val listAdapter = RecAdapter(mutableListOf())
        val recyclerView = findViewById<RecyclerView>(R.id.rec_view).also {
            it.layoutManager = LinearLayoutManager(this@MainActivity)
            it.adapter = listAdapter
        }
        req.addOnSuccessListener {
            val rows = it["resp_data"].asJsonArray
            rows.forEach { r ->
                val row = r.asJsonObject
                listAdapter.insertNewData(row)
            }
            recyclerView.scrollToPosition(0)
            Log.i("Hehe", it.toString())
        }

        req.addOnFailureListener {
            Log.i("Gagal", it.toString())
        }

        req.send()
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                req.send(true)
                delay(1000)
            }
        }
    }
}

private class Get(context: Context) {
    private var onSuccess: ((res: JsonObject)->Unit)? = null
    private var onFailure: ((e: Exception)->Unit)? = null
    private var execDelay = 1L

    fun addOnSuccessListener(listener: ((res: JsonObject)->Unit)) { onSuccess = listener }
    fun addOnFailureListener(listener: ((e: Exception)->Unit)) { onFailure = listener }
    fun addExecutionDelay(delay: Long) { execDelay = delay }

    // custom method for API call into my server
    fun send(satuan: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = if (!satuan) URL else URL_SATUAN
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.useCaches = false
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val response = InputStreamReader(conn.inputStream)
                val jsonResponse = JsonParser.parseString(response.readText()).asJsonObject
                conn.disconnect()
                CoroutineScope(Dispatchers.Main).launch {
                    delay(execDelay)
                    try {
                        onSuccess?.invoke(jsonResponse)
                    } catch (e: Exception) {
                        onFailure?.invoke(e)
                    }
                }
            } catch (e: Exception) {
                CoroutineScope(Dispatchers.Main).launch { onFailure?.invoke(e) }
            }
        }
    }

    companion object {
        private const val URL = "http://pjc.lintasborneosukses.com:5555/ambilData"
        private const val URL_SATUAN = "http://pjc.lintasborneosukses.com:5555/ambilDataSatuan"
    }
}

private class RecAdapter(
    var dataSet: MutableList<JsonObject>
) : RecyclerView.Adapter<RecAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.list_title)
        val subTitle: TextView = view.findViewById(R.id.list_subtitle)
        val time: TextView = view.findViewById(R.id.list_time)
        init {
            view.setOnClickListener {  }
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(
            R.layout.recycler_view_item, viewGroup, false
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        try {
            dataSet[position].also {
                val row = it.asJsonObject
                viewHolder.title.text = row["title"].asString
                viewHolder.subTitle.text = row["sub"].asString
                viewHolder.time.text = row["time"].asString
                Log.i("Hehe", it.toString())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun getItemCount() = dataSet.size

    fun insertNewData(data: JsonObject) {
        if (dataSet.isNotEmpty()) {
            if (dataSet[0].get("id") != data.get("id")) {
                dataSet.add(0, data)
                notifyItemInserted(0)
            }
        } else {
            dataSet.add(0, data)
            notifyItemInserted(0)
        }
    }

    fun editData(position: Int, key: String, value: Int) {
        dataSet[position].asJsonObject.addProperty(key, value)
        notifyItemChanged(position)
    }

    fun removeData(position: Int) {
        dataSet.removeAt(position)
        notifyItemRemoved(position)
    }
}





