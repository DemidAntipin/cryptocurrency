package com.example.myapplication

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.math.BigDecimal


class RateCheckService : Service() {
    val handler = Handler(Looper.getMainLooper())
    var rateCheckAttempt = 0
    lateinit var startRate: BigDecimal
    lateinit var targetRate: BigDecimal
    val rateCheckInteractor = RateCheckInteractor()

    val rateCheckRunnable: Runnable = Runnable {
        requestAndCheckRate()
    }

    private fun requestAndCheckRate() {
        if (rateCheckAttempt >= RATE_CHECK_ATTEMPTS_MAX) {
            Log.d(TAG, "Maximum attempts reached. Stopping service.")
            stopSelf()
            return
        }

        GlobalScope.launch(Dispatchers.IO) {
            val currentRate = rateCheckInteractor.requestRate()

            if (currentRate.USD > 0) {
                try {
                    val currentRateUSD = BigDecimal.valueOf(currentRate.USD)
                    Log.d(TAG, "Current rate as BigDecimal: $currentRateUSD")
                    if (currentRateUSD >= targetRate) {
                        Log.d(TAG, "Target rate reached: $currentRateUSD >= $targetRate")
                        handler.post {
                            Toast.makeText(this@RateCheckService, "BTC Достигнули $targetRate$", Toast.LENGTH_SHORT).show()
                        }
                        stopSelf()
                    } else {
                        rateCheckAttempt++
                        Log.d(TAG, "Attempt $rateCheckAttempt: Target rate not reached yet.")
                        handler.postDelayed(rateCheckRunnable, RATE_CHECK_INTERVAL)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process current rate: ${currentRate.USD}", e)
                    handler.post {
                        Toast.makeText(this@RateCheckService, "Failed to get rate!", Toast.LENGTH_SHORT).show()
                    }
                    stopSelf()
                }
            } else {
                Log.w(TAG, "Received invalid rate. Stopping service.")
                handler.post {
                    Toast.makeText(this@RateCheckService, "Failed to get rate!", Toast.LENGTH_SHORT).show()
                }
                stopSelf()
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startRateString = intent?.getStringExtra(ARG_START_RATE)
        val targetRateString = intent?.getStringExtra(ARG_TARGET_RATE)
        if (!startRateString.isNullOrEmpty() && !targetRateString.isNullOrEmpty()) {
            try {
                startRate = BigDecimal(startRateString)
                targetRate = BigDecimal(targetRateString)

                Log.d(TAG, "onStartCommand startRate = $startRate targetRate = $targetRate")

                rateCheckAttempt = 0
                handler.post(rateCheckRunnable)
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Failed to parse start or target rate", e)
                Toast.makeText(this, "Invalid start or target rate!", Toast.LENGTH_SHORT).show()
                stopSelf()
            }


        } else {
            Log.e(TAG, "Invalid start or target rate")
            Toast.makeText(this, "Invalid start or target rate!", Toast.LENGTH_SHORT).show()
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(rateCheckRunnable)
    }


    companion object {
        const val TAG = "RateCheckService"
        const val RATE_CHECK_INTERVAL = 5000L
        const val RATE_CHECK_ATTEMPTS_MAX = 100

        const val ARG_START_RATE = "ARG_START_RATE"
        const val ARG_TARGET_RATE = "ARG_TARGET_RATE"

        fun startService(context: Context, startRate: String, targetRate: String) {
            context.startService(Intent(context, RateCheckService::class.java).apply {
                putExtra(ARG_START_RATE, startRate.replace(',', '.'))
                putExtra(ARG_TARGET_RATE, targetRate.replace(',', '.'))
            })
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, RateCheckService::class.java))
        }
    }



}