package com.example.a1p

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket

class SocketClient(context: Context) {
    private var socket: Socket? = null
    private var printWriter: PrintWriter? = null
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var serviceHost: String? = null
    private var servicePort: Int = 0

    fun discoverServices() {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                println("Start discover")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                println("Found service: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == "_example._tcp.") {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            println("Resolve Fail: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            println("Service Resolved: ${serviceInfo.host.hostAddress}")
                            serviceHost = serviceInfo.host.hostAddress
                            servicePort = serviceInfo.port
                            connect()
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {
            }

            override fun onDiscoveryStopped(serviceType: String) {
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            }
        }

        nsdManager.discoverServices(
            "_example._tcp.",
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun connect() {
        Thread {
            try {
                serviceHost?.let {
                    socket = Socket(it, servicePort)
                    printWriter = PrintWriter(socket!!.getOutputStream(), true)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun sendIntArray(data: IntArray) {
        Thread {
            try {
                val dataString = '['+data.joinToString(",")+']'
                printWriter?.println(dataString)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    fun close() {
        try {
            printWriter?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
