package com.example.a1p

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit

class TimeCounter {
    private var startTime: Long = 0L
    private var elapsedTime: Long = 0L
    private var isRunning: Boolean = false
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val updateInterval: Long = 3L
    fun start() {
        isRunning = !isRunning
        if (isRunning) {
            startTime = System.currentTimeMillis() - elapsedTime
            handler.post(object : Runnable {
                override fun run() {
                    elapsedTime = System.currentTimeMillis() - startTime
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) % 60
                    val mSeconds = TimeUnit.MILLISECONDS.toMillis(elapsedTime) % 60
                    val formattedTime =
                        String.format("%02d:%02d:%02d", minutes, seconds, mSeconds)
                    GlobalData.time.value = formattedTime
                    if (isRunning) {
                        handler.postDelayed(this, updateInterval)
                    }
                }
            })
        }
    }

    fun reset() {
        isRunning = false
        startTime = 0L
        elapsedTime = 0L
        GlobalData.time.value = "00:00:00"
        handler.removeCallbacksAndMessages(null)
    }
}

