package com.example.myapplication

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.google.gson.Gson

class RateCheckInteractor {
    val networkClient = NetworkClient()

    suspend fun requestRate(): BTC {
        return withContext(Dispatchers.IO) {
            val result = networkClient.request(MainViewModel.getUsdRateUrl())
            val gson = Gson()
            val parsed: BTC = gson.fromJson(result, BTC::class.java)
            parsed
        }
    }
}