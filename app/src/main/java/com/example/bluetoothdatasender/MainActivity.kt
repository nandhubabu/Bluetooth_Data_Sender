package com.example.bluetoothdatasender

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private lateinit var btnSelectDevice: Button
    private lateinit var tvSelectedDevice: TextView
    private lateinit var btnConnect: Button
    private lateinit var btnSendData: Button
    private lateinit var btnNotificationAccess: Button
    private lateinit var tvStatus: TextView

    private var selectedMacAddress: String? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelectDevice = findViewById(R.id.btnSelectDevice)
        tvSelectedDevice = findViewById(R.id.tvSelectedDevice)
        btnConnect = findViewById(R.id.btnConnect)
        btnSendData = findViewById(R.id.btnSendData)
        tvStatus = findViewById(R.id.tvStatus)

        // Add a hidden button or just check on start for notification access
        btnNotificationAccess = Button(this).apply {
            text = "Enable Maps Reader"
            setOnClickListener {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        }
        (findViewById<android.widget.LinearLayout>(R.id.main_layout)).addView(btnNotificationAccess)

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        btnSelectDevice.setOnClickListener {
            if (checkBluetoothPermissions()) {
                if (bluetoothAdapter?.isEnabled == true) {
                    showDeviceSelectionDialog()
                } else {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        startActivity(enableBtIntent)
                    }
                }
            } else {
                requestBluetoothPermissions()
            }
        }

        btnConnect.setOnClickListener {
            selectedMacAddress?.let { mac ->
                connectToEsp32(mac)
            }
        }

        btnSendData.setOnClickListener {
            val navCommand = "NAV:TEST_CMD=READY\n"
            com.example.bluetoothdatasender.BluetoothManager.sendData(navCommand)
            Toast.makeText(this, "Test Command Sent", Toast.LENGTH_SHORT).show()
        }

        // Register for discovery results
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    private fun showDeviceSelectionDialog() {
        val deviceListStrings = mutableListOf<String>()
        val deviceMap = mutableMapOf<String, String>()

        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val pairedDevices = bluetoothAdapter?.bondedDevices
            pairedDevices?.forEach { device ->
                val info = "${device.name ?: "Unknown"} (Paired)\n${device.address}"
                deviceListStrings.add(info)
                deviceMap[info] = device.address
            }
        }

        discoveredDevices.forEach { device ->
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                val name = device.name ?: "Unknown Device"
                val info = "$name\n${device.address}"
                if (!deviceListStrings.contains(info)) {
                    deviceListStrings.add(info)
                    deviceMap[info] = device.address
                }
            }
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceListStrings)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select ESP32 Device")

        builder.setAdapter(adapter) { _, which ->
            val selectedInfo = deviceListStrings[which]
            selectedMacAddress = deviceMap[selectedInfo]
            tvSelectedDevice.text = "Selected: $selectedInfo"
            btnConnect.isEnabled = true
            bluetoothAdapter?.cancelDiscovery()
        }

        builder.setNeutralButton("Scan for New", null)
        builder.setNegativeButton("Cancel") { dialog, _ ->
            bluetoothAdapter?.cancelDiscovery()
            dialog.dismiss()
        }

        val dialog = builder.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            startScanning(adapter, deviceListStrings, deviceMap)
        }
    }

    private fun startScanning(adapter: ArrayAdapter<String>, list: MutableList<String>, map: MutableMap<String, String>) {
        if (checkBluetoothPermissions()) {
            if (bluetoothAdapter?.isEnabled == false) {
                Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show()
                return
            }
            discoveredDevices.clear()
            Toast.makeText(this, "Scanning started...", Toast.LENGTH_SHORT).show()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                if (bluetoothAdapter?.isDiscovering == true) {
                    bluetoothAdapter?.cancelDiscovery()
                }
                val success = bluetoothAdapter?.startDiscovery()
                if (success == false) {
                    Toast.makeText(this, "Could not start scan. Is Location (GPS) turned ON?", Toast.LENGTH_LONG).show()
                }
            }

            currentScanningAdapter = adapter
            currentScanningList = list
            currentScanningMap = map
        } else {
            requestBluetoothPermissions()
        }
    }

    private var currentScanningAdapter: ArrayAdapter<String>? = null
    private var currentScanningList: MutableList<String>? = null
    private var currentScanningMap: MutableMap<String, String>? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND, BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                            val name = it.name ?: "Unknown Device"
                            val address = it.address
                            val info = "$name\n$address"

                            if (!discoveredDevices.contains(it) && currentScanningList?.contains(info) == false) {
                                discoveredDevices.add(it)
                                currentScanningList?.add(info)
                                currentScanningMap?.put(info, address)
                                currentScanningAdapter?.notifyDataSetChanged()
                                Toast.makeText(context, "Found: $name", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(context, "Search started...", Toast.LENGTH_SHORT).show()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Toast.makeText(context, "Scanning finished", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun connectToEsp32(macAddress: String) {
        tvStatus.text = "Status: Connecting..."
        btnConnect.isEnabled = false

        thread {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return@thread
                }

                val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(macAddress)
                val socket = device?.createRfcommSocketToServiceRecord(sppUuid)

                socket?.connect()

                // Update Singleton
                com.example.bluetoothdatasender.BluetoothManager.bluetoothSocket = socket
                com.example.bluetoothdatasender.BluetoothManager.outputStream = socket?.outputStream

                runOnUiThread {
                    tvStatus.text = "Status: Connected"
                    btnSendData.isEnabled = true
                    Toast.makeText(this, "Connected successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = "Status: Connection Failed"
                    btnConnect.isEnabled = true
                    Toast.makeText(this, "Failed to connect. Check ESP32 power.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestBluetoothPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        ActivityCompat.requestPermissions(this, permissions, 101)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(receiver)
            com.example.bluetoothdatasender.BluetoothManager.bluetoothSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
