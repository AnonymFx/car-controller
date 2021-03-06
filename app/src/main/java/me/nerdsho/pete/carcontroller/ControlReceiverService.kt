package me.nerdsho.pete.carcontroller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import org.jetbrains.anko.doAsync
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.Future

class ControlReceiverService : ForegroundService() {
    companion object {
        private const val VALID_INPUT_PATTERN = "m(-)?(100|\\d(\\d)?)s(-)?(100|\\d(\\d)?)"
        const val UDP_PORT = 3001
    }

    override val notificationId = 2
    override val notificationChannelId = "control_receiver_service"
    override val friendlyServiceName = "Control Receiver Service"

    private var listenerThread: Future<Unit>? = null
    private var udpSocket: DatagramSocket? = null
    private var controlService: CarControlService? = null
    private val serviceConnection: ServiceConnection = object: ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            controlService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CarControlService.CarControlBinder
            controlService = binder.getService()
        }

    }

    override fun onCreate() {
        super.onCreate()

        startListening()
        bindControlService()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startListening() {
        listenerThread = doAsync {
            Log.d(this.javaClass.canonicalName, "Listening on port $UDP_PORT")
            udpSocket = DatagramSocket(UDP_PORT)
            while (udpSocket?.isClosed != true) {
                val packet = DatagramPacket(ByteArray(11), 11)
                udpSocket?.receive(packet)
                val newLineIndex = packet.data.indexOf("\n".toByteArray(Charsets.UTF_8)[0])
                if(newLineIndex == -1) {
                    val message = String(packet.data)
                    Log.d(this.javaClass.name, "No newline found in command $message, skipping")
                    continue
                }
                val message = String(packet.data, 0, packet.data.indexOf("\n".toByteArray()[0]))
                if (!VALID_INPUT_PATTERN.toRegex().matches(message)) {
                    Log.d(this.javaClass.canonicalName, "Received invalid message $message")
                    continue
                }
                Log.d(this.javaClass.canonicalName, "Received valid message: $message")
                controlService?.sendCommand(message)
            }
        }
    }

    private fun bindControlService() {
        Intent(this, CarControlService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun stopListening() {
        udpSocket?.close()
        listenerThread?.cancel(true)
    }
}
