package com.example.bluetoothdatasender

import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread

object BluetoothManager {
    var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null

    fun sendData(message: String) {
        if (outputStream == null) return

        thread {
            try {
                outputStream?.write(message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }
}
