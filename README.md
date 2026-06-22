# Bluetooth Maps to ESP32 Sender

A specialized Android application designed to bridge Google Maps navigation directions to an ESP32 or other Bluetooth-enabled microcontrollers.

## 🚀 Key Features
- **Live Google Maps Integration**: Uses a `NotificationListenerService` to "read" active navigation instructions and beam them to your hardware.
- **Background Persistence**: The Bluetooth connection stays alive in the background while you navigate using the Google Maps app.
- **Smart Device Selection**: Browse both already **paired** devices and **scan** for new nearby Bluetooth hardware.
- **Automatic Formatting**: Navigation instructions are parsed and sent as easy-to-read strings: `MAPS: [Direction] | [Distance/Street]`.
- **Modern Compatibility**: Fully optimized for Android 12, 13, and 14 with appropriate permission handling.

## 🛠 How to Use
1. **Prepare Hardware**: Ensure your ESP32 is running a Bluetooth Serial sketch (SPP).
2. **Connect**:
   - Open the app and tap **"Select / Scan for Device"**.
   - Select your hardware from the list.
   - Tap **"Connect to Selected"**.
3. **Enable Reader**:
   - Tap the **"Enable Maps Reader"** button.
   - In the system list that appears, find **"BluetoothDataSender"** and toggle it to **ON**.
4. **Navigate**: Open Google Maps and start a navigation route. Your directions will now automatically stream to your hardware.

## 🔒 Permissions Required
- **Bluetooth Connect & Scan**: To find and talk to your hardware.
- **Location (GPS)**: Required by Android for Bluetooth scanning discovery.
- **Notification Access**: Required to "secretly" read directions from the Google Maps notification bar.

## 📂 Project Structure
- `BluetoothManager.kt`: A thread-safe Singleton that manages the shared Bluetooth socket.
- `MapNotificationListener.kt`: The background service that intercepts and parses Maps notifications.
- `MainActivity.kt`: The user interface for connection management and device discovery.

## 📜 License
This project is licensed under the MIT License.
