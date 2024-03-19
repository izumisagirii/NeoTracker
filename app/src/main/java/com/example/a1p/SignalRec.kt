package com.example.a1p

import android.Manifest
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream


open class SignalRec {

    @Deprecated("Better CPP ver")
    private class WavHeader(
        val sampleRate: Int,
        val bitsPerSample: Int,
        val channels: Int,
        val audioDataSize: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val chunkSize = 36 + audioDataSize
        val subChunk1Size = 16
        val audioFormat = 1

        fun write(outputStream: FileOutputStream) {
            writeString(outputStream, "RIFF")
            writeInt(outputStream, chunkSize)
            writeString(outputStream, "WAVE")
            writeString(outputStream, "fmt ")
            writeInt(outputStream, subChunk1Size)
            writeShort(outputStream, audioFormat.toShort())
            writeShort(outputStream, channels.toShort())
            writeInt(outputStream, sampleRate)
            writeInt(outputStream, byteRate)
            writeShort(outputStream, blockAlign.toShort())
            writeShort(outputStream, bitsPerSample.toShort())
            writeString(outputStream, "data")
            writeInt(outputStream, audioDataSize)
        }

        private fun writeString(outputStream: FileOutputStream, value: String) {
            for (element in value) {
                outputStream.write(element.code)
            }
        }

        private fun writeInt(outputStream: FileOutputStream, value: Int) {
            outputStream.write(value and 0xFF)
            outputStream.write(value shr 8 and 0xFF)
            outputStream.write(value shr 16 and 0xFF)
            outputStream.write(value shr 24 and 0xFF)
        }

        private fun writeShort(outputStream: FileOutputStream, value: Short) {
            outputStream.write(value.toInt() and 0xFF)
            outputStream.write(value.toInt() shr 8 and 0xFF)
        }
    }

    init {
        val audioManager = GlobalData.activity.getSystemService(AudioManager::class.java)
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        audioDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }?.let {
            val mainMicId = it.id
            // Now you have the ID of the main microphone
        }
    }


    private external fun startPlayback()
    private external fun stopPlayback()
    private external fun startRecord()
    private external fun stopRecord()



    open fun getCacheDir():File {
        return GlobalData.activity.cacheDir
    }

    open fun getAbsolutePath():String {
        return GlobalData.activity.cacheDir.absolutePath
    }

    fun start(): Boolean {
        Log.d("SignalRec", "Start")
        if (ActivityCompat.checkSelfPermission(
                GlobalData.activity, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                GlobalData.activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1
            )
            return false
        }


        startPlayback()
        startRecord()

        return true
    }

    fun stop(): Boolean {
        Log.d("SignalRec", "Stop")

        stopRecord()
        stopPlayback()
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "audio/wav"
        val uri = FileProvider.getUriForFile(GlobalData.activity, "com.example.a1p.fileprovider", File(getCacheDir(),"output.wav"))
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        val chooserIntent = Intent.createChooser(shareIntent, "Share wav file")
        startActivity(GlobalData.activity, chooserIntent, null)


        return true
    }
}