package com.example.bluetoothdatasender

import android.bluetooth.BluetoothSocket
import java.io.IOException
import java.io.OutputStream
import kotlin.concurrent.thread

object BluetoothManager {
    @Volatile
    var bluetoothSocket: BluetoothSocket? = null
    @Volatile
    var outputStream: OutputStream? = null

    fun sendData(message: String) {
        val stream = outputStream ?: return

        thread {
            try {
                stream.write(message.toByteArray())
            } catch (e: IOException) {
                e.printStackTrace()
                // If write fails, connection might be dead
                disconnect()
            }
        }
    }

    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    fun disconnect() {
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bluetoothSocket = null
            outputStream = null
        }
    }
}
