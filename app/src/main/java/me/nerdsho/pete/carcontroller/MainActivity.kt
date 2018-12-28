package me.nerdsho.pete.carcontroller

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.SeekBar
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.lang.Thread.sleep
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
    private var carControlService: CarControlService? = null
    private var sendingTask: Future<Unit>? = null
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as CarControlService.CarControlBinder
            carControlService = binder.getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            carControlService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Intent(this, CarControlService::class.java).also { intent -> startForegroundService(intent) }
        motor_control.progress = motor_control.max/2
        steering_control.progress = steering_control.max/2

//        scanLeDevice(true)
    }

    override fun onStart() {
        super.onStart()
        Intent(this, CarControlService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        carControlService = null
        sendingTask?.cancel(true)
        sendingTask = null
    }

    private fun startBackgroundTask(): Future<Unit> {
        return doAsync {
            while (true) {
//                Log.d("motor_control", (motor_control.progress - 100).toString())
//                Log.d("steering_control", (steering_control.progress - 100).toString())
                sleep(100)
                carControlService?.sendCommand("m${motor_control.progress - motor_control.max/2}s${-(steering_control.progress - steering_control.max/2)}")
            }
        }
    }

//    private fun log(logEntry: String) {
//        runOnUiThread { log_view.text = "$logEntry\n\n${log_view.text}" }
//    }

    fun onClickSend(view: View) {
        val command = "${command_view.text}"
        carControlService?.sendCommand(command)
    }

    fun onClickStop(view: View) {
        motor_control.progress = motor_control.max/2
        steering_control.progress = steering_control.max/2
        sendingTask?.cancel(true)
        sendingTask = null
    }

    fun onClickStart(view: View) {
        sendingTask = startBackgroundTask()
    }

    fun onClickRestartService(view: View) {
        stopService(Intent(this, CarControlService::class.java))
        Intent(this, CarControlService::class.java).also { intent -> startForegroundService(intent) }
        Intent(this, CarControlService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
}
