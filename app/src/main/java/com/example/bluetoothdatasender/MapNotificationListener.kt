package com.example.bluetoothdatasender

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MapNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName == "com.google.android.apps.maps") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // Standard Google Maps notification check:
            // Often Title is the turn direction and Text is the distance/street.
            if (title.isNotEmpty() || text.isNotEmpty()) {
                val command = "MAPS: $title | $text\n"
                Log.d("MapListener", "Sending to ESP32: $command")
                BluetoothManager.sendData(command)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optional: Could send a "CLEAR" command to ESP32 when navigation stops
    }
}
