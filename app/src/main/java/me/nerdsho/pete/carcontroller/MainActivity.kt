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
        private const val MAX_MOTOR = 100
        private const val MAX_STEERING = 100
        private const val REFRESH_INTERVAL = 100L
    }

    private var carControlService: CarControlService? = null
    private var sendingTask: Future<Unit>? = null
    private var currentSteering = 0
    private var currentMotor = 0
    //    private var currentCommand: String = "m0s0"
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

        steering_control_view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateSteering(event, v)
                }
                MotionEvent.ACTION_MOVE -> {
                    updateSteering(event, v)
                }
                MotionEvent.ACTION_UP -> {
                    currentSteering = 0
                    sendCommand(currentMotor, 0)
                }
            }
            true
        }
        motor_control_view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    updateMotor(event, v)
                }
                MotionEvent.ACTION_MOVE -> {
                    updateMotor(event, v)
                }
                MotionEvent.ACTION_UP -> {
                    currentMotor = 0
                    sendCommand(0, currentSteering)
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

    override fun onResume() {
        super.onResume()
        startBackgroundTask()
    }

    override fun onPause() {
        super.onPause()
        stopBackgroundTask()
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
                sleep(REFRESH_INTERVAL)
                sendCommand(currentMotor, currentSteering)
            }
        }
    }

    private fun stopBackgroundTask() {
        sendingTask?.cancel(true)
        sendingTask = null
    }

    private fun sendCommand(motor: Int, steering: Int) {
        val command = "m${motor}s$steering"
        runOnUiThread {
            command_debug?.text = command
        }
        carControlService?.sendCommand(command)
    }

    private fun updateMotor(event: MotionEvent, v: View) {
        var relativeY = event.y
        if (relativeY < 0) {
            relativeY = 0F
        } else if (relativeY > v.height) {
            relativeY = v.height.toFloat()
        }


        val newMotorAbsolute = Math.round(
            Math.pow(((2 * relativeY - v.height) / v.height).toDouble(), 2.0) * MAX_MOTOR
        ).toInt()

//        currentMotor =
        currentMotor = if (relativeY > v.height / 2) {
            - newMotorAbsolute
        } else {
            newMotorAbsolute
        }
    }

    private fun updateSteering(event: MotionEvent, v: View) {
        var relativeX = event.x
        if (relativeX < 0) {
            relativeX = 0F
        } else if (relativeX > v.width) {
            relativeX = v.width.toFloat()
        }
        currentSteering = Math.round(((relativeX - v.width / 2.0) / v.width) * MAX_STEERING * 2).toInt()
    }

}
