package me.nerdsho.pete.carcontroller

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
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
                    log("Connected to gatt $gatt, status $status")
                    this@MainActivity.gatt = gatt
                    this@MainActivity.gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    log("Disconnected from gatt $gatt, status $status")
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            log("OnCharacteristicWrite with status $status")
        }

        // New services discovered
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            log("Service discovered with status $status")
            if (carControl == null) {
//                gatt.services.find { service ->
//                    service.uuid.toString() == CAR_CONTROL_SERVICE_UUID
//                }?.characteristics?.forEach { char -> Log.d("blub", char.uuid.toString())}
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
                carController?.connectGatt(this@MainActivity, false, connectGattCallback)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            log("Error code $errorCode when scanning for BLE devices")
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanLeDevice(true)
    }

    override fun onStop() {
        super.onStop()
        gatt?.close()
        carControl = null
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

    private fun log(logEntry: String) {
        runOnUiThread { log_view.text = "$logEntry\n\n${log_view.text}" }
    }

    fun onClickSend(view: View) {
        val command = "${command_view.text}\n"
        log("Sending command: ${command_view.text}")

        log("carController is $carControl")

        carControl?.setValue(command)
        log("Characteristic value is ${String(carControl!!.value)}")

        val status = gatt?.writeCharacteristic(carControl)
        log("Wrote characteristic with status $status")
    }
}
