package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    lateinit var viewModel: MainViewModel
    lateinit var textRateUSD: TextView
    lateinit var textRateJPY: TextView
    lateinit var textRateRUB: TextView
    lateinit var rootView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MainViewModel.fsym=getString(R.string.coin)
        MainViewModel.tsym= resources.getStringArray(R.array.currency).toMutableList()
        initViewModel()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()

    }

    fun initViewModel() {
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        viewModel.usdRate.observe(this, {
            textRateUSD.text = "$$it"
        })
        viewModel.rubRate.observe(this, {
            textRateRUB.text = "$it RUB"
        })
        viewModel.jpyRate.observe(this, {
            textRateJPY.text = "$it JPY"
        })

        viewModel.onCreate()
    }

    fun initView() {
        textRateUSD = findViewById(R.id.textUsdRate)
        textRateJPY = findViewById(R.id.textJpyRate)
        textRateRUB = findViewById(R.id.textRubRate)
        rootView = findViewById(R.id.rootView)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            viewModel.onRefreshClicked()
        }

        findViewById<Button>(R.id.btnSubscribeToRate).setOnClickListener {
            val targetRate = findViewById<EditText>(R.id.textUsdRubRate).text.toString()
            val startRate = viewModel.usdRate.value
            if (targetRate.isNotEmpty() && startRate?.isNotEmpty() == true) {
                RateCheckService.stopService(this)
                RateCheckService.startService(this, startRate, targetRate)
            } else if (targetRate.isEmpty()) {
                Snackbar.make(rootView, R.string.target_rate_empty, Snackbar.LENGTH_SHORT).show()
            } else if (startRate.isNullOrEmpty()) {
                Snackbar.make(rootView, R.string.current_rate_empty, Snackbar.LENGTH_SHORT).show()
            }
        }

    }
}