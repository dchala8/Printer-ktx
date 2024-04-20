package com.khairo.escposprinter.connection.tcp

import android.content.Context
import com.khairo.exeption.PrintingException.FINISH_PRINTER_DISCONNECTED
import com.khairo.exeption.onException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayList

/**
 * Create un instance of TcpConnection.
 *
 * @param address IP address of the device
 * @param port    Port of the device
 */
class TcpConnection(private val address: String, private val port: Int, private val adr: String, private val idbill: String) : TcpDeviceConnection() {
    private var socket: Socket? = null
    private var exceptions: ArrayList<ArrayList<String>> = arrayListOf();



    fun getExceptions() : ArrayList<ArrayList<String>> { return exceptions }

    /**
     * Check if the TCP device is connected by socket.
     *
     * @return true if is connected
     */
    override fun isConnected(): Boolean =
        socket != null && socket!!.isConnected && super.isConnected()

    /**
     * Start socket connection with the TCP device.
     */
    override suspend fun connect(context: Context): TcpConnection {
        if (this.isConnected()) return this

        try {
            socket = Socket()
            socket!!.connect(InetSocketAddress(InetAddress.getByName(address), port))
            stream = socket!!.getOutputStream()
            data = ByteArray(0)

        } catch (e: IOException) {
            e.printStackTrace()
            socket = null
            stream = null
            exceptions.add(arrayListOf(adr,idbill))
            onException(context, FINISH_PRINTER_DISCONNECTED)
//            throw EscPosConnectionException("Unable to connect to TCP device.")
        }
        return this
    }

    /**
     * Close the socket connection with the TCP device.
     */
    override suspend fun disconnect(): TcpConnection {
        data = ByteArray(0)
        if (stream != null) {
            try {
                stream!!.close()
                stream = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        if (socket != null) {
            try {
                socket!!.close()
                socket = null
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return this
    }
}