/*
 * Copyright 2018 Naoki Seto(@Lionas)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.lionas.androidthings.sensor.mcp3008

import android.support.annotation.VisibleForTesting
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.SpiDevice
import com.google.android.things.pio.SpiDevice.BIT_JUSTIFICATION_MSB_FIRST
import java.io.IOException

/**
 * Analog to Digital Converter Driver (MCP3008)
 * Reference from https://github.com/PaulTR/AndroidThingsMCP3008ADC
 * @author Naoki Seto(@Lionas)
 */
class MCP3008 : ADConverter, AutoCloseable {

    private lateinit var spiName: String
    private lateinit var spiDevice: SpiDevice

    @VisibleForTesting
    var peripheralManager: PeripheralManager? = null

    override fun setSpi(spiName: String) {
        this.spiName = spiName
    }

    @Throws(IOException::class)
    override fun register() {
        if (peripheralManager == null) {
            peripheralManager = PeripheralManager.getInstance()
        }
        spiDevice = peripheralManager!!.openSpiDevice(spiName)
        spiDevice.setMode(SpiDevice.MODE0)
        spiDevice.setFrequency(1350000) // 1.35MHz
        spiDevice.setBitsPerWord(8)
        spiDevice.setBitJustification(BIT_JUSTIFICATION_MSB_FIRST) // MSB first
    }

    @Throws(IOException::class)
    override fun readAdc(channels: IntArray): IntArray {
        val results = IntArray(channels.size)
        for ((index, channel) in channels.withIndex()) {
            if (channel < 0 || channel > 7) {
                throw IOException("ADC channel must be between 0 and 7")
            }
            val transferData = createTransferData(channel)
            val receivedData = ByteArray(3)
            spiDevice.transfer(transferData, receivedData, receivedData.size)
            results[index] =extractReceivedData(receivedData)
        }
        return results
    }

    @VisibleForTesting
    fun createTransferData(channel: Int): ByteArray {
        val txData = ByteArray(3)
        txData[0] = 0x01.toByte()
        txData[1] = (channel shl 0x4 or 0x80).toByte()
        txData[2] = 0x00.toByte()
        return txData
    }

    @VisibleForTesting
    fun extractReceivedData(receivedData: ByteArray): Int {
        return (receivedData[1].toInt() and 0x03 shl 0x08) or (receivedData[2].toInt() and 0xFF)
    }

    override fun close() {
        unregister()
    }

    override fun unregister() {
        try {
            spiDevice.close()
        } catch (ignored: IOException) {
        } catch (ignored: RuntimeException) {
        }
    }

}
