package com.opencode.mobile.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DiscoveredServer(
    val url: String,
    val host: String,
    val port: Int
)

suspend fun discoverServers(timeout: Long = 3000): List<DiscoveredServer> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DiscoveredServer>()
    try {
        val message = byteArrayOf(0x4F, 0x43, 0x44, 0x49, 0x53, 0x43, 0x4F, 0x56, 0x45, 0x52)
        val socket = DatagramSocket()
        socket.broadcast = true
        socket.soTimeout = timeout.toInt()

        val packet = DatagramPacket(message, message.size, InetAddress.getByName("255.255.255.255"), 4097)
        socket.send(packet)

        val buffer = ByteArray(256)
        while (true) {
            try {
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length)
                val host = receivePacket.address.hostAddress
                results.add(DiscoveredServer("http://$host:4096", host, 4096))
            } catch (_: java.net.SocketTimeoutException) {
                break
            }
        }
        socket.close()
    } catch (_: Exception) {}
    results
}
