package com.example.a1p

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
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
        AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    private val BUFFER_SIZE_PLAYER = AudioTrack.getMinBufferSize(SAMPLE_RATE,CHANNEL_CONFIG,AUDIO_FORMAT)
//    private val FRAME_LENGTH = 512
//    private val WAV_HEADER_SIZE = 44
    private val UPDATE_INTERVAL = 5L
    private var RUNNING = true
    private lateinit var recorder: AudioRecord
//    private lateinit var player: AudioTrack
    private val handler: Handler = Handler(Looper.getMainLooper())
//    private val handler_play: Handler = Handler(Looper.getMainLooper())

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

    }

    private external fun startPlayback()
    private external fun stopPlayback()
    fun start(): Boolean {
        Log.d("SignalRec", "Start")

        /*
        ＲＲＲＲ　　　ＥＥＥＥＥ　　　ＣＣＣ　　　　ＯＯＯ　　　ＲＲＲＲ　　　ＤＤＤＤ
        Ｒ　　　Ｒ　　Ｅ　　　　　　Ｃ　　　Ｃ　　Ｏ　　　Ｏ　　Ｒ　　　Ｒ　　Ｄ　　　Ｄ
        Ｒ　　　Ｒ　　Ｅ　　　　　　Ｃ　　　Ｃ　　Ｏ　　　Ｏ　　Ｒ　　　Ｒ　　Ｄ　　　Ｄ
        Ｒ　　　Ｒ　　Ｅ　　　　　　Ｃ　　　　　　Ｏ　　　Ｏ　　Ｒ　　　Ｒ　　Ｄ　　　Ｄ
        ＲＲＲＲ　　　ＥＥＥＥＥ　　Ｃ　　　　　　Ｏ　　　Ｏ　　ＲＲＲＲ　　　Ｄ　　　Ｄ
        Ｒ　　Ｒ　　　Ｅ　　　　　　Ｃ　　　　　　Ｏ　　　Ｏ　　Ｒ　　Ｒ　　　Ｄ　　　Ｄ
        Ｒ　　　Ｒ　　Ｅ　　　　　　Ｃ　　　Ｃ　　Ｏ　　　Ｏ　　Ｒ　　　Ｒ　　Ｄ　　　Ｄ
        Ｒ　　　Ｒ　　Ｅ　　　　　　Ｃ　　　Ｃ　　Ｏ　　　Ｏ　　Ｒ　　　Ｒ　　Ｄ　　　Ｄ
        Ｒ　　　Ｒ　　ＥＥＥＥＥ　　　ＣＣＣ　　　　ＯＯＯ　　　Ｒ　　　Ｒ　　ＤＤＤＤ
         */

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

//        /*
//        ＰＰＰＰ　　　Ｌ　　　　　　　　Ａ　　　　Ｙ　　　Ｙ　　ＥＥＥＥＥ　　ＲＲＲＲ
//        Ｐ　　　Ｐ　　Ｌ　　　　　　　　Ａ　　　　Ｙ　　　Ｙ　　Ｅ　　　　　　Ｒ　　　Ｒ
//        Ｐ　　　Ｐ　　Ｌ　　　　　　　Ａ　Ａ　　　Ｙ　　　Ｙ　　Ｅ　　　　　　Ｒ　　　Ｒ
//        Ｐ　　　Ｐ　　Ｌ　　　　　　　Ａ　Ａ　　　　Ｙ　Ｙ　　　Ｅ　　　　　　Ｒ　　　Ｒ
//        ＰＰＰＰ　　　Ｌ　　　　　　　Ａ　Ａ　　　　Ｙ　Ｙ　　　ＥＥＥＥＥ　　ＲＲＲＲ
//        Ｐ　　　　　　Ｌ　　　　　　　ＡＡＡ　　　　　Ｙ　　　　Ｅ　　　　　　Ｒ　　Ｒ
//        Ｐ　　　　　　Ｌ　　　　　　Ａ　　　Ａ　　　　Ｙ　　　　Ｅ　　　　　　Ｒ　　　Ｒ
//        Ｐ　　　　　　Ｌ　　　　　　Ａ　　　Ａ　　　　Ｙ　　　　Ｅ　　　　　　Ｒ　　　Ｒ
//        Ｐ　　　　　　ＬＬＬＬＬ　　Ａ　　　Ａ　　　　Ｙ　　　　ＥＥＥＥＥ　　Ｒ　　　Ｒ
//         */
//
//        Deprecated because of performance issues
//
//        player = AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, BUFFER_SIZE_PLAYER, AudioTrack.MODE_STREAM)
//        val buffer_player = ByteArray(BUFFER_SIZE_PLAYER)
//        player.write(buffer,0,len)
//        player.play()
//
//        var len = 0
//        while (fis.available() > 0) {
//            len = fis.read(buffer)
//            audioTrack.write(buffer, 0, len)
//        }
//        audioTrack.stop()
//        audioTrack.release()
//        fis.close()



        startPlayback()
        recorder.startRecording()
        RUNNING = true
        /*
        Ｕ　　　Ｕ　　ＰＰＰＰ　　　ＤＤＤＤ　　　　　Ａ　　　　ＴＴＴＴＴ　　ＥＥＥＥＥ
        Ｕ　　　Ｕ　　Ｐ　　　Ｐ　　Ｄ　　　Ｄ　　　　Ａ　　　　　　Ｔ　　　　Ｅ
        Ｕ　　　Ｕ　　Ｐ　　　Ｐ　　Ｄ　　　Ｄ　　　Ａ　Ａ　　　　　Ｔ　　　　Ｅ
        Ｕ　　　Ｕ　　Ｐ　　　Ｐ　　Ｄ　　　Ｄ　　　Ａ　Ａ　　　　　Ｔ　　　　Ｅ
        Ｕ　　　Ｕ　　ＰＰＰＰ　　　Ｄ　　　Ｄ　　　Ａ　Ａ　　　　　Ｔ　　　　ＥＥＥＥＥ
        Ｕ　　　Ｕ　　Ｐ　　　　　　Ｄ　　　Ｄ　　　ＡＡＡ　　　　　Ｔ　　　　Ｅ
        Ｕ　　　Ｕ　　Ｐ　　　　　　Ｄ　　　Ｄ　　Ａ　　　Ａ　　　　Ｔ　　　　Ｅ
        Ｕ　　　Ｕ　　Ｐ　　　　　　Ｄ　　　Ｄ　　Ａ　　　Ａ　　　　Ｔ　　　　Ｅ
        　ＵＵＵ　　　Ｐ　　　　　　ＤＤＤＤ　　　Ａ　　　Ａ　　　　Ｔ　　　　ＥＥＥＥＥ
         */

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
        stopPlayback()
        return true

    }
}