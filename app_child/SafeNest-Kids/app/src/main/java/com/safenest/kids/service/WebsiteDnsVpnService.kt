package com.safenest.kids.service

import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.safenest.kids.network.WebsitePolicySyncResponse
import com.safenest.kids.util.PrefsHelper
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

/**
 * DNS-only website filtering VPN. The first release intentionally filters hostnames,
 * not HTTPS paths or page contents. Unsupported/non-DNS traffic is not claimed as filtered.
 */
class WebsiteDnsVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private val executor = Executors.newSingleThreadExecutor()
    @Volatile private var running = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(android.R.drawable.ic_secure)
                .setContentTitle("Layngo website protection")
                .setContentText("DNS host filtering is active")
                .setOngoing(true)
                .build()
        )
        if (!running) startFiltering()
        return START_STICKY
    }

    private fun startFiltering() {
        val snapshot = try {
            PrefsHelper(this).getWebsitePolicySnapshotJson()?.let {
                Gson().fromJson(it, WebsitePolicySyncResponse::class.java)
            }
        } catch (error: Exception) {
            Log.e(TAG, "Invalid website policy snapshot", error)
            null
        } ?: run {
            Log.w(TAG, "Website filtering unavailable: no valid policy snapshot")
            stopSelf()
            return
        }

        val prefs = PrefsHelper(this)
        if (VpnService.prepare(this) != null) {
            prefs.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_DENIED)
            Log.w(TAG, "Website filtering unavailable: VPN permission is not granted")
            stopSelf()
            return
        }
        vpnInterface = try {
            Builder()
                .setSession("Layngo Website Protection")
                .addAddress("10.0.0.2", 32)
                .addRoute("10.0.0.2", 32)
                .addDnsServer("10.0.0.2")
                .establish()
        } catch (error: Exception) {
            Log.e(TAG, "Could not establish the website-filtering VPN", error)
            prefs.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_FAILED)
            stopSelf()
            return
        }
        if (vpnInterface == null) {
            prefs.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_FAILED)
            Log.e(TAG, "Could not establish the website-filtering VPN")
            stopSelf()
            return
        }
        prefs.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_ACTIVE)

        val engine = WebsitePolicyEngine(snapshot)
        running = true
        executor.execute { packetLoop(engine) }
        Log.i(TAG, "Website DNS filtering started for policy ${snapshot.policyId} v${snapshot.version}")
    }

    private fun packetLoop(engine: WebsitePolicyEngine) {
        val descriptor = vpnInterface ?: return
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val packetBuffer = ByteArray(32767)
        try {
            while (running) {
                val size = input.read(packetBuffer)
                if (size <= 0) continue
                val requestPacket = packetBuffer.copyOf(size)
                val dnsQuery = Ipv4Udp.extractDnsPayload(requestPacket) ?: continue
                val host = DnsPacket.readHost(dnsQuery) ?: continue
                val decision = engine.decide(host)
                val dnsResponse = if (decision.blocked) {
                    Log.d(TAG, "Blocked website host=$host reason=${decision.reason}")
                    DnsPacket.nxDomain(dnsQuery)
                } else {
                    forwardDns(dnsQuery)
                }
                output.write(Ipv4Udp.wrapResponse(requestPacket, dnsResponse))
            }
        } catch (error: Exception) {
            if (running) Log.e(TAG, "Website DNS loop stopped unexpectedly", error)
        } finally {
            input.close()
            output.close()
        }
    }

    private fun forwardDns(query: ByteArray): ByteArray {
        DatagramSocket().use { socket ->
            protect(socket)
            socket.soTimeout = 2500
            val server = InetAddress.getByName("1.1.1.1")
            socket.send(DatagramPacket(query, query.size, server, 53))
            val response = ByteArray(32767)
            val packet = DatagramPacket(response, response.size)
            socket.receive(packet)
            return response.copyOf(packet.length)
        }
    }

    override fun onDestroy() {
        running = false
        executor.shutdownNow()
        vpnInterface?.close()
        vpnInterface = null
        PrefsHelper(this).setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_UNAVAILABLE)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    "Website protection",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    private object Ipv4Udp {
        fun extractDnsPayload(packet: ByteArray): ByteArray? {
            if (packet.size < 28 || ((packet[0].toInt() ushr 4) and 0x0f) != 4) return null
            val ihl = (packet[0].toInt() and 0x0f) * 4
            if (packet.size < ihl + 8 || (packet[9].toInt() and 0xff) != 17) return null
            val destinationPort = u16(packet, ihl + 2)
            if (destinationPort != 53) return null
            return packet.copyOfRange(ihl + 8, packet.size)
        }

        fun wrapResponse(request: ByteArray, dnsResponse: ByteArray): ByteArray {
            val ihl = (request[0].toInt() and 0x0f) * 4
            val sourcePort = u16(request, ihl)
            val totalLength = ihl + 8 + dnsResponse.size
            val response = ByteArray(totalLength)
            response[0] = 0x45
            response[1] = 0
            putU16(response, 2, totalLength)
            response[4] = request[4]
            response[5] = request[5]
            response[6] = 0
            response[7] = 0
            response[8] = 64
            response[9] = 17
            System.arraycopy(request, 16, response, 12, 4)
            System.arraycopy(request, 12, response, 16, 4)
            putU16(response, 10, checksum(response, 0, ihl))
            putU16(response, ihl, 53)
            putU16(response, ihl + 2, sourcePort)
            putU16(response, ihl + 4, 8 + dnsResponse.size)
            putU16(response, ihl + 6, 0)
            System.arraycopy(dnsResponse, 0, response, ihl + 8, dnsResponse.size)
            return response
        }

        private fun u16(bytes: ByteArray, offset: Int): Int =
            ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)

        private fun putU16(bytes: ByteArray, offset: Int, value: Int) {
            bytes[offset] = (value ushr 8).toByte()
            bytes[offset + 1] = value.toByte()
        }

        private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
            var sum = 0L
            var index = offset
            while (index < offset + length) {
                sum += (((bytes[index].toInt() and 0xff) shl 8) or (bytes[index + 1].toInt() and 0xff))
                index += 2
            }
            while ((sum ushr 16) != 0L) sum = (sum and 0xffff) + (sum ushr 16)
            return sum.inv().toInt() and 0xffff
        }
    }

    private object DnsPacket {
        fun readHost(packet: ByteArray): String? {
            if (packet.size < 17) return null
            val qdCount = ((packet[4].toInt() and 0xff) shl 8) or (packet[5].toInt() and 0xff)
            if (qdCount < 1) return null
            var offset = 12
            val labels = mutableListOf<String>()
            while (offset < packet.size) {
                val length = packet[offset].toInt() and 0xff
                offset++
                if (length == 0) break
                if (length > 63 || offset + length > packet.size) return null
                labels += packet.copyOfRange(offset, offset + length).toString(Charsets.US_ASCII)
                offset += length
            }
            return labels.joinToString(".").takeIf { it.isNotBlank() }
        }

        fun nxDomain(query: ByteArray): ByteArray = errorResponse(query, 0x8183)

        private fun errorResponse(query: ByteArray, flags: Int): ByteArray {
            val questionEnd = questionEnd(query) ?: return query
            val result = query.copyOfRange(0, questionEnd)
            result[2] = (flags ushr 8).toByte()
            result[3] = flags.toByte()
            result[6] = 0
            result[7] = 0
            result[8] = 0
            result[9] = 0
            result[10] = 0
            result[11] = 0
            return result
        }

        private fun questionEnd(query: ByteArray): Int? {
            var offset = 12
            while (offset < query.size) {
                val length = query[offset].toInt() and 0xff
                offset++
                if (length == 0) break
                if (length > 63 || offset + length > query.size) return null
                offset += length
            }
            if (offset + 4 > query.size) return null
            return offset + 4
        }
    }

    companion object {
        private const val TAG = "WebsiteDnsVpnService"
        private const val NOTIFICATION_CHANNEL = "website_protection"
        private const val NOTIFICATION_ID = 4201

        fun startIfPermissionGranted(context: Context): Boolean {
            if (VpnService.prepare(context) != null) return false
            ContextCompat.startForegroundService(
                context,
                Intent(context, WebsiteDnsVpnService::class.java)
            )
            return true
        }
    }
}
