package me.nerdsho.pete.carcontroller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import java.lang.Thread.sleep
import java.util.concurrent.Future

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MAX_MOTOR = 50F
        private const val MAX_STEERING = 100F
    }

    private var carControlService: CarControlService? = null
    private var sendingTask: Future<Unit>? = null
    private var currentCommand: String = "m0s0"
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

        joystick.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startBackgroundTask()
                }
                MotionEvent.ACTION_MOVE -> {
                    var relativeX = event.x
                    if (relativeX < 0) {
                        relativeX = 0F
                    } else if (relativeX > v.width) {
                        relativeX = v.width.toFloat()
                    }
                    val steeringValue = Math.round(((relativeX - v.width/2.0)/v.width) * MAX_STEERING)

                    var relativeY = event.y
                    if (relativeY < 0) {
                        relativeY = 0F
                    } else if (relativeY > v.width) {
                        relativeY = v.width.toFloat()
                    }
                    val motorValue = Math.round(((relativeY - v.height/2.0)/v.height) * MAX_MOTOR)

                    currentCommand = "m${motorValue}s$steeringValue"
                }
                MotionEvent.ACTION_UP -> {
                    sendCommand("m0s0")
                    stopBackgroundTask()
                }
            }
            true
        }
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

    private fun startBackgroundTask() {
        if (sendingTask != null) {
            return
        }

        sendingTask = doAsync {
            while (true) {
//                Log.d("motor_control", (motor_control.progress - 100).toString())
//                Log.d("steering_control", (steering_control.progress - 100).toString())
                sleep(100)
                sendCommand(currentCommand)
            }
        }
    }

    private fun stopBackgroundTask() {
        sendingTask?.cancel(true)
        sendingTask = null
    }

    private fun sendCommand(command: String) {
        runOnUiThread {
            command_debug?.text = command
        }
        carControlService?.sendCommand(command)
    }

//    private fun log(logEntry: String) {
//        runOnUiThread { log_view.text = "$logEntry\n\n${log_view.text}" }
//    }

    fun onClickSend(view: View) {
        val command = "${command_view.text}"
        sendCommand(command)
    }

    fun onClickStop(view: View) {
        sendCommand("m0s0")
        stopBackgroundTask()
    }

    fun onClickStart(view: View) {
        startBackgroundTask()
    }

    fun onClickRestartService(view: View) {
        stopService(Intent(this, CarControlService::class.java))
        Intent(this, CarControlService::class.java).also { intent -> startForegroundService(intent) }
        Intent(this, CarControlService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
}
