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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

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
                    val isPositiveChange = currentRateUSD - startRate >= targetRate
                    val isNegativeChange = currentRateUSD - startRate <= -targetRate

                    if (isPositiveChange || isNegativeChange) {
                        val message = "Курс BTC изменился на $$targetRate"
                        val icon = if (isPositiveChange) R.drawable.up else R.drawable.down
                        sendNotification("Изменение курса", message, icon)
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

    private fun sendNotification(title: String, message: String, iconResId: Int) {
        val notificationId = 1
        val channelId = "BTC_CHANNEL_ID"

        // Создание Intent для открытия приложения при нажатии на уведомление
        val intent = Intent(this@RateCheckService, MainActivity::class.java)

        // Указываем FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Создание уведомления
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(iconResId) // Установка иконки уведомления в зависимости от изменения
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Создание канала уведомлений для Android 8.0 и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "BTC Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Отправка уведомления
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(notificationId, notificationBuilder.build())
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