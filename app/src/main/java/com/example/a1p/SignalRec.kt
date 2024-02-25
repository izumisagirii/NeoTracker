package com.example.a1p

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


open class SignalRec {
    private val SAMPLE_RATE = 48000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE =
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) // 缓冲区大小
    private val FRAME_LENGTH = 512
    private val WAV_HEADER_SIZE = 44
    private val UPDATE_INTERVAL = 5L
    private var RUNNING = true
    private lateinit var recorder: AudioRecord
    private val handler: Handler = Handler(Looper.getMainLooper())

    private class WavHeader(
        val sampleRate: Int,
        val bitsPerSample: Int,
        val channels: Int,
        val audioDataSize: Int
    ) {
        // 计算一些参数
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val chunkSize = 36 + audioDataSize
        val subChunk1Size = 16
        val audioFormat = 1

        // 将wav文件头写入输出流
        fun write(outputStream: FileOutputStream) {
            // 写入RIFF标识
            writeString(outputStream, "RIFF")
            // 写入块大小
            writeInt(outputStream, chunkSize)
            // 写入WAVE标识
            writeString(outputStream, "WAVE")
            // 写入fmt标识
            writeString(outputStream, "fmt ")
            // 写入子块1大小
            writeInt(outputStream, subChunk1Size)
            // 写入音频格式
            writeShort(outputStream, audioFormat.toShort())
            // 写入声道数
            writeShort(outputStream, channels.toShort())
            // 写入采样率
            writeInt(outputStream, sampleRate)
            // 写入比特率
            writeInt(outputStream, byteRate)
            // 写入块对齐
            writeShort(outputStream, blockAlign.toShort())
            // 写入采样位数
            writeShort(outputStream, bitsPerSample.toShort())
            // 写入data标识
            writeString(outputStream, "data")
            // 写入音频数据大小
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

        // 将一个短整数写入输出流，使用小端字节序
        private fun writeShort(outputStream: FileOutputStream, value: Short) {
            outputStream.write(value.toInt() and 0xFF)
            outputStream.write(value.toInt() shr 8 and 0xFF)
        }
    }

    init {

    }

    fun start(): Boolean {
        Log.d("SignalRec", "Start")



        if (ActivityCompat.checkSelfPermission(
                GlobalData.activity, Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(
                GlobalData.activity, arrayOf(Manifest.permission.RECORD_AUDIO), 1
            )
            return false
        }

        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE
        )
//        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG,AUDIO_FORMAT)
        val buffer = ByteArray(BUFFER_SIZE)
        val outputStream = ByteArrayOutputStream()

        recorder.startRecording()
        RUNNING = true
        handler.post(object : Runnable {
            override fun run() {
                val read = recorder.read(buffer, 0, BUFFER_SIZE)
//                Log.d("bufferSize","$BUFFER_SIZE")
                outputStream.write(buffer, 0, read)
                if (RUNNING) {
                    handler.postDelayed(this, UPDATE_INTERVAL)
                    return
                }
                recorder.stop()
                recorder.release()


                val audioData = outputStream.toByteArray()
                outputStream.close()

                val tempFile = File.createTempFile("temp", ".wav")

                try {
                    val fileOutputStream = FileOutputStream(tempFile)
                    val wavHeader = WavHeader(SAMPLE_RATE, 16, 1, audioData.size)
                    wavHeader.write(fileOutputStream)
                    fileOutputStream.write(audioData)
                    fileOutputStream.close()
                } catch (e: Exception) {
                    Log.e("Tracker", "Error writing wav data to temp file", e)
                }

                val fileUri = FileProvider.getUriForFile(
                    GlobalData.activity,
                    "com.example.a1p.fileprovider",
                    tempFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "audio/wav"
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                val chooserIntent = Intent.createChooser(shareIntent, "Share wav file")
                startActivity(GlobalData.activity, chooserIntent, null)


//                val wavFile = File("audio.wav")
//                val fileOutputStream = FileOutputStream(wavFile)
//                val wavHeader = WavHeader(SAMPLE_RATE, 16, 1, audioData.size)
//                wavHeader.write(fileOutputStream)
//                fileOutputStream.write(audioData)
//                fileOutputStream.close()

//                val intent = Intent(Intent.ACTION_SEND)
//                intent.type = "audio/wav"
//                val uri = FileProvider.getUriForFile(this, "com.example.fileprovider", file)
//
//                intent.putExtra(Intent.EXTRA_STREAM, uri)
//                startActivity(Intent.createChooser(intent, "audio.wav"))

            }
        })
        return true
    }

    fun stop(): Boolean {
        Log.d("SignalRec", "Stop")
        RUNNING = false
        return true
    }
}