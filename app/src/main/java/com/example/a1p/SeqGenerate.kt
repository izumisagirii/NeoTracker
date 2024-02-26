package com.example.a1p

import android.content.res.AssetManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.properties.Delegates

class TestLayer {
    fun setAssetManager(assetManager: AssetManager) {
        SetAssetManager(assetManager)
    }

    companion object {
        // Native 方法：设置 AAssetManager
        private external fun SetAssetManager(assetManager: AssetManager)
    }
}
// Deprecated, use cpp interface instead
data class Complex(val real: Double, val imag: Double) {
    fun times(other: Complex): Complex {
        val realPart = this.real * other.real - this.imag * other.imag
        val imagPart = this.real * other.imag + this.imag * other.real
        return Complex(realPart, imagPart)
    }
}

class SeqGenerate(private val fc: Int) {
    private var carryRate by Delegates.notNull<Int>()
    private var array: Array<Complex>
    private lateinit var multiplyArray: Array<Complex>
    private var pArray by Delegates.notNull<Int>()
    private var pCarry by Delegates.notNull<Int>()
    private var boundArray by Delegates.notNull<Int>()

    init {
        carryRate = fc
        val json = readJsonFromAssets(GlobalData.signalFName)
        array = jsonToComplexArray(json)
        boundArray = array.size
        generateCarrierArray(fc)
        pArray = 0
        pCarry = 0
    }

    private fun getNew():Double{
        val amp = array[pArray].times(multiplyArray[pCarry]).real
        pArray++
        pCarry++
        if(pArray == boundArray){
            pArray = 0
        }
        if(pCarry == 48000){
            pCarry = 0
        }
        return amp
    }
    fun getNewInt16(): Int {
        val value = getNew()
//        if (value < -1 || value > 1) {
//            throw IllegalArgumentException("value must be between -1 and 1")
//        }
        val mappedValue = (value - (-1)) / (1 - (-1))
        val scaledValue = mappedValue * 65535
        return scaledValue.roundToInt()
    }
    private fun readJsonFromAssets(fileName: String): String {
        val assetManager = GlobalData.activity.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            stringBuilder.append(line)
        }
        bufferedReader.close()
        inputStream.close()
        return stringBuilder.toString()
    }

    private fun jsonToComplexArray(json: String): Array<Complex> {
        val type = object : TypeToken<List<Complex>>() {}.type
        val list = Gson().fromJson<List<Complex>>(json, type)
        return list.toTypedArray()
    }

    private fun generateCarrierArray(fc: Int){
        multiplyArray = Array(48000) { Complex(0.0, 0.0) }
        for (i in 0 until 48000) {
            val radians:Double = 2 * i * PI *(fc.toDouble()/48000)
            array[i] = Complex(cos(radians), sin(radians))
        }
    }
}



