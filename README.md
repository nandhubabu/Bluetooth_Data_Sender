# Bluetooth Data Sender

An Android application designed to connect to an ESP32 (or other Bluetooth SPP devices) and send data over Serial Port Profile (SPP).

## Features
- **Device Selection**: Choose from a list of already paired Bluetooth devices.
- **Scanning**: Scan for new nearby Bluetooth devices.
- **Connection Management**: Connect and disconnect from a selected device.
- **Data Transmission**: Send test pings or custom data strings to the connected device.
- **Modern Permissions**: Fully compatible with Android 12+ (API 31) permission model.

## Getting Started
1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on an Android device.
4. Ensure your ESP32 has Bluetooth Serial enabled.

## Permissions Required
- `BLUETOOTH_CONNECT`: To connect to paired devices.
- `BLUETOOTH_SCAN`: To scan for new devices.
- `ACCESS_FINE_LOCATION`: Required for Bluetooth scanning on many Android versions.

## License
This project is licensed under the MIT License.
