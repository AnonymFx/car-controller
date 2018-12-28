package me.nerdsho.pete.carcontroller

import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder

class CarControllService: Service() {

    companion object {
        private const val ENABLE_BLUETOOTH_REQUEST = 0
        private const val CAR_CONTROLLER_MAC = "FB:F4:20:A6:08:40"
        private const val CAR_CONTROL_SERVICE_UUID = "00001234-0000-1000-8000-00805f9b34fb"
        private const val CAR_CONTROL_WRITE_CHARACTERISTIC_UUID = "00001235-0000-1000-8000-00805f9b34fb"
        private val SCAN_PERIOD: Long = 10000
    }

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
                    this@CarControllService.gatt = gatt
                    this@CarControllService.gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
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
            }
        }
    }
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result?.device?.address == CAR_CONTROLLER_MAC) {
                val carController = result.device
                carController?.connectGatt(this@CarControllService, false, connectGattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }
    }

    override fun onCreate() {
        super.onCreate()
        scanLeDevice(true)
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}