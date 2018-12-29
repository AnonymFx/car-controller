package me.nerdsho.pete.carcontroller

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class CarControlService : ForegroundService() {
    companion object {
        private const val CAR_CONTROLLER_MAC = "FB:F4:20:A6:08:40"
        private const val CAR_CONTROL_SERVICE_UUID = "00001234-0000-1000-8000-00805f9b34fb"
        private const val CAR_CONTROL_WRITE_CHARACTERISTIC_UUID = "00001235-0000-1000-8000-00805f9b34fb"
        private const val SCAN_PERIOD: Long = 10000
    }

    override val notificationId: Int = 1
    override val notificationChannelId: String = "car_control_service"
    override val friendlyServiceName: String = "Car control service"

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
                BluetoothProfile.STATE_DISCONNECTING -> {
                    gatt.close()
                    this@CarControlService.gatt = null
                    this@CarControlService.carControl = null
                }
            }
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (carControl == null) {
                carControl = gatt.services.find { service ->
                    service.uuid.toString() == CAR_CONTROL_SERVICE_UUID
                }
                    ?.characteristics?.find { characteristic -> characteristic.uuid.toString() == CAR_CONTROL_WRITE_CHARACTERISTIC_UUID }
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
    }

    override fun onCreate() {
        super.onCreate()

        scanLeDevice(true)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun scanLeDevice(enable: Boolean) {
        when (enable) {
            true -> {
                if (!isScanning) {
                    handler.postDelayed({
                        isScanning = false
                        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
                    }, SCAN_PERIOD)
                    isScanning = true
                    bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
                }
            }
            else -> {
                if (isScanning) {
                    isScanning = false
                    bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
                }
            }
        }
    }

    fun sendCommand(command: String) {
        if (carControl == null) {
            scanLeDevice(true)
        }

        Log.d(this.javaClass.canonicalName, "Sending command $command")
        carControl?.setValue("$command\n")
        val status = gatt?.writeCharacteristic(carControl)
        if (status == false) {
            gatt?.disconnect()
            gatt = null
            carControl = null
        }
    }

    inner class CarControlBinder : Binder() {
        fun getService(): CarControlService = this@CarControlService
    }
}