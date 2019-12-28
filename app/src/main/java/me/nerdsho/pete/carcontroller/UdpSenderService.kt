package me.nerdsho.pete.carcontroller

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.jetbrains.anko.doAsync
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketAddress

class UdpSenderService : ForegroundService() {
    companion object {
//                private const val IP_ADDRESS = "151.217.233.162"
        private const val IP_ADDRESS = "151.217.117.87"
    }

    override val notificationId: Int = 3
    override val notificationChannelId: String = "udp_sender_service"
    override val friendlyServiceName: String = "UDP sender service"

    override fun onBind(intent: Intent?): IBinder? {
        return UdpSenderBinder()
    }


    fun sendCommand(command: String) {
        doAsync {
            try {
                val udpSocket = DatagramSocket()
                val bytes = "$command\n".toByteArray()
                val packet = DatagramPacket(
                    bytes,
                    bytes.size,
                    InetAddress.getByName(IP_ADDRESS),
                    ControlReceiverService.UDP_PORT
                )
                Log.d(this.javaClass.name, "Sending udp packet")
                udpSocket.send(packet)
            } catch (e: Exception) {
                Log.e(this.javaClass.name, e.toString())
            }
        }
    }

    inner class UdpSenderBinder : Binder() {
        fun getService(): UdpSenderService = this@UdpSenderService
    }
}