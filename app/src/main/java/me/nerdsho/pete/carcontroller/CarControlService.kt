package me.nerdsho.pete.carcontroller

import android.app.*
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class CarControlService : Service() {
    companion object {
        private const val ENABLE_BLUETOOTH_REQUEST = 0
        private const val CAR_CONTROLLER_MAC = "FB:F4:20:A6:08:40"
        private const val CAR_CONTROL_SERVICE_UUID = "00001234-0000-1000-8000-00805f9b34fb"
        private const val CAR_CONTROL_WRITE_CHARACTERISTIC_UUID = "00001235-0000-1000-8000-00805f9b34fb"
        private val SCAN_PERIOD: Long = 10000
        private val ONGOING_NOTIFICATION_ID = 1
    }

    private val binder = CarControlBinder()
    private val bluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    private val handler = Handler()
    private var isScanning = false
    private var carControl: BluetoothGattCharacteristic? = null
    private var gatt: BluetoothGatt? = null
    private var connectGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    this@CarControlService.gatt = gatt
                    this@CarControlService.gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    this@CarControlService.gatt = null
                    this@CarControlService.carControl = null
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (carControl == null) {
                carControl = gatt.services.find { service ->
                    service.uuid.toString() == CAR_CONTROL_SERVICE_UUID
                }
                    ?.characteristics?.find { characteristic -> characteristic.uuid.toString() == CAR_CONTROL_WRITE_CHARACTERISTIC_UUID }
                carControl?.setValue("5\n")
                gatt.writeCharacteristic(carControl)
            }
        }
    }
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result?.device?.address == CAR_CONTROLLER_MAC) {
                val carController = result.device
                carController?.connectGatt(this@CarControlService, false, connectGattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Toast.makeText(this, "Car control service starting...", Toast.LENGTH_SHORT).show()

        val channelId = createNotificationChannel("car_control_service", "Car control service")

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, channelId)
            .setContentTitle("Car control service")
            .setContentIntent(pendingIntent)
            .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)

        scanLeDevice(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Car control service stopping...", Toast.LENGTH_SHORT).show()
    }

//    override fun onHandleIntent(intent: Intent?) {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//        Toast.makeText(this, "Received intent", Toast.LENGTH_SHORT).show()
//    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                handler.postDelayed({
                    isScanning = false
                    bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
                }, SCAN_PERIOD)
                isScanning = true
                bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
            }
            else -> {
                isScanning = false
                bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
            }
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    fun sendCommand(command: String) {
        if (carControl == null) {
            scanLeDevice(true)
        }
        carControl?.setValue("$command\n")
        val status = gatt?.writeCharacteristic(carControl)
        Log.d("SentCommand", "Sent $command to characteristic $carControl with status $status")
    }

    inner class CarControlBinder : Binder() {
        fun getService(): CarControlService = this@CarControlService
    }
}