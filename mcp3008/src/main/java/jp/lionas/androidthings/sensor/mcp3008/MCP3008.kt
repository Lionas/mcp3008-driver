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
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import java.io.IOException

/**
 * Analog to Digital Converter Driver (MCP3008)
 * Reference from https://github.com/PaulTR/AndroidThingsMCP3008ADC
 * @author Naoki Seto(@Lionas)
 */
class MCP3008(
        private val csPin: String,
        private val clockPin: String,
        private val mosiPin: String,
        private val misoPin: String
) : ADConverter, AutoCloseable {

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

    @Throws(IOException::class, NullPointerException::class)
    override fun register() {
        if (peripheralManager == null) {
            peripheralManager = PeripheralManager.getInstance()
        }
        mClockPin = peripheralManager!!.openGpio(clockPin)
        mCsPin = peripheralManager!!.openGpio(csPin)
        mMosiPin = peripheralManager!!.openGpio(mosiPin)
        mMisoPin = peripheralManager!!.openGpio(misoPin)

        mClockPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        mCsPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        mMosiPin!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        mMisoPin!!.setDirection(Gpio.DIRECTION_IN)
    }

    @Throws(IOException::class, NullPointerException::class)
    override fun readAdc(channels: IntArray): IntArray {
        val results = IntArray(channels.size)
        for ((index, channel) in channels.withIndex()) {
            if (channel < 0 || channel > 7) {
                throw IOException("ADC channel must be between 0 and 7")
            }
            initReadState()
            initChannelSelect(channel)
            results[index] = valueFromSelectedChannel
        }
        return results
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
