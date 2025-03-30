package com.example.myapplication

import android.location.Location
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    val usdRate = MutableLiveData<String>()
    val jpyRate = MutableLiveData<String>()
    val rubRate = MutableLiveData<String>()
    val rateCheckInteractor = RateCheckInteractor()

    fun onCreate() {
        refreshRate()
    }

    fun onRefreshClicked() {
        refreshRate()
    }

    private fun refreshRate() {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val rate = rateCheckInteractor.requestRate()
                Log.d(TAG, "usdRate = ${rate.USD}")
                Log.d(TAG, "rubRate = ${rate.RUB}")
                Log.d(TAG, "jpyRate = ${rate.JPY}")

                usdRate.value = String.format("%.2f", rate.USD)
                jpyRate.value = String.format("%.2f", rate.JPY)
                rubRate.value = String.format("%.2f", rate.RUB)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении курса: ${e.message}")
                // Вы можете обновить LiveData с сообщением об ошибке, если это необходимо
            }
        }
    }

    companion object {
        const val TAG = "MainViewModel"
        var fsym: String = ""
        var tsym = mutableListOf<String>();
        const val apikey = "755c2e7f8f4a0fc3b24e0e66803ee9bb2ff754fc142fe1869afc9da7379ca719"
        fun getUsdRateUrl(): String {
            return "https://min-api.cryptocompare.com/data/price?fsym=$fsym&tsyms=${tsym.joinToString(",")}&apikey=$apikey"
        }
    }
}