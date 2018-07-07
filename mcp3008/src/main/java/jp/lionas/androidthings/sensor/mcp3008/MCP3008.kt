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
class MCP3008(
        private val adcChannelPin: Int
) : ADConverter, AutoCloseable {

    private var spiName: String? = null
    private lateinit var spiDevice: SpiDevice
    private lateinit var csPin: String
    private lateinit var clockPin: String
    private lateinit var mosiPin: String
    private lateinit var misoPin: String

    @VisibleForTesting
    var mCsPin: Gpio? = null

    @VisibleForTesting
    var mClockPin: Gpio? = null

    @VisibleForTesting
    var mMosiPin: Gpio? = null

    @VisibleForTesting
    var mMisoPin: Gpio? = null

    @VisibleForTesting
    var peripheralManager: PeripheralManager? = null

    @VisibleForTesting
    val valueFromSelectedChannel: Int
        @Throws(IOException::class, NullPointerException::class)
        get() {
            var value = 0x0
            for (i in 0..11) {
                toggleClock()
                value = value shl 0x1
                if (mMisoPin!!.value) {
                    value = value or 0x1
                }
            }
            mCsPin!!.value = true
            value = value shr 0x1 // first bit is 'null', so drop it

            return value
        }

    override fun setSpi(spiName: String) {
        this.spiName = spiName
    }

    override fun setGpioPorts(csPin: String,
                            clockPin: String,
                            mosiPin: String,
                            misoPin: String) {
        this.csPin = csPin
        this.clockPin = clockPin
        this.mosiPin = mosiPin
        this.misoPin = misoPin
    }

    @Throws(IOException::class, NullPointerException::class)
    override fun register() {
        if (peripheralManager == null) {
            peripheralManager = PeripheralManager.getInstance()
        }
        if (spiName != null) {
            spiDevice = peripheralManager!!.openSpiDevice(spiName)
            spiDevice.setMode(SpiDevice.MODE0)
            spiDevice.setFrequency(1350000) // 1.35MHz
            spiDevice.setBitsPerWord(8)
            spiDevice.setBitJustification(BIT_JUSTIFICATION_MSB_FIRST) // MSB first
        } else {
            mClockPin = peripheralManager!!.openGpio(clockPin)
            mCsPin = peripheralManager!!.openGpio(csPin)
            mMosiPin = peripheralManager!!.openGpio(mosiPin)
            mMisoPin = peripheralManager!!.openGpio(misoPin)
            mClockPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mCsPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mMosiPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mMisoPin!!.setDirection(Gpio.DIRECTION_IN)
        }
    }

    @Throws(IOException::class, NullPointerException::class)
    override fun readAdc(): Int {
        if (adcChannelPin < 0 || adcChannelPin > 7) {
            throw IOException("ADC channel must be between 0 and 7")
        }

        return if (spiName !=  null) {
            val transferData = createTransferData(adcChannelPin)
            val receivedData = ByteArray(3)
            spiDevice.transfer(transferData, receivedData, receivedData.size)
            extractReceivedData(receivedData)
        } else {
            initReadState()
            initChannelSelect(adcChannelPin)
            valueFromSelectedChannel
        }

    }

    private fun createTransferData(channel: Int): ByteArray {
        val txData = ByteArray(3)
        txData[0] = 0x01.toByte()
        txData[1] = (channel shl 0x4 or 0x80).toByte()
        txData[2] = 0x00.toByte()
        return txData
    }

    private fun extractReceivedData(receivedData: ByteArray): Int {
        return (receivedData[1].toInt() and 0x03 shl 0x08) or (receivedData[2].toInt() and 0xFF)
    }

    @VisibleForTesting
    @Throws(IOException::class, NullPointerException::class)
    fun initReadState() {
        mCsPin!!.value = true
        mClockPin!!.value = false
        mCsPin!!.value = false
    }

    @VisibleForTesting
    @Throws(IOException::class, NullPointerException::class)
    fun initChannelSelect(channel: Int) {
        var command = channel
        command = command or 0x18 // start bit + single-ended bit
        command = command shl 0x3 // we only need to send 5 bits

        for (i in 0..4) {
            mMosiPin!!.value = command and 0x80 != 0x0
            command = command shl 0x1
            toggleClock()
        }
    }

    @Throws(IOException::class, NullPointerException::class)
    private fun toggleClock() {
        mClockPin!!.value = true
        mClockPin!!.value = false
    }

    override fun close() {
        unregister()
    }

    override fun unregister() {
        try {
            mCsPin!!.close()
            mClockPin!!.close()
            mMisoPin!!.close()
            mMosiPin!!.close()
            mCsPin = null
            mClockPin = null
            mMisoPin = null
            mMosiPin = null
        } catch (ignored: IOException) {
        } catch (ignored: RuntimeException) {
        }
    }

}
