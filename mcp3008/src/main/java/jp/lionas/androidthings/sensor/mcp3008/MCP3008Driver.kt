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

import android.hardware.Sensor
import com.google.android.things.userdriver.UserDriverManager
import com.google.android.things.userdriver.sensor.UserSensor
import com.google.android.things.userdriver.sensor.UserSensorDriver
import com.google.android.things.userdriver.sensor.UserSensorReading
import java.io.IOException
import java.util.*

/**
 * Analog to Digital Converter User Driver (MCP3008)
 * @author Naoki Seto(@Lionas)
 */
class MCP3008Driver(
        val channels: IntArray = intArrayOf(DEFAULT_CHANNEL),
        private val useSpi: Boolean = false
) : AutoCloseable {

    companion object {
        const val DRIVER_NAME = "MCP3008"
        const val DEFAULT_PIN_CS = "GPIO1_IO10"
        const val DEFAULT_PIN_CLOCK = "GPIO6_IO13"
        const val DEFAULT_PIN_MOS_IN = "GPIO6_IO12"
        const val DEFAULT_PIN_MOS_OUT = "GPIO5_IO00"
        const val DEFAULT_CHANNEL = 0
        const val DEFAULT_SPI_NAME = "SPI3.0"
    }

    private val adc: ADConverter = MCP3008()
    private var userSensorDriver: CustomUserSensorDriver? = null

    private var spiName: String = DEFAULT_SPI_NAME
    private var cs: String = DEFAULT_PIN_CS
    private var clock: String = DEFAULT_PIN_CLOCK
    private var mosIn: String = DEFAULT_PIN_MOS_IN
    private var mosOut: String = DEFAULT_PIN_MOS_OUT

    override fun close() {
        unregister()
    }

    fun setGpioPins(csPin: String, clockPin: String, mosiPin: String, misoPin: String) {
        this.cs = csPin
        this.clock = clockPin
        this.mosIn = mosiPin
        this.mosOut = misoPin
    }

    fun setSpiName(spiName: String) {
        this.spiName = spiName
    }

    /**
     * Register a [UserSensor] that pipes adc into the Android SensorManager.
     * @see .unregister
     */
    @Throws(IOException::class)
    fun register() {
        if (useSpi) {
            adc.setSpi(spiName)
        } else {
            adc.setGpioPorts(cs, clock, mosIn, mosOut)
        }
        userSensorDriver = CustomUserSensorDriver()
        userSensorDriver?.let {
            adc.register()
            UserDriverManager.getInstance().registerSensor(it.userSensor)
        }
    }

    /**
     * Unregister the adc [UserSensor].
     */
    fun unregister() {
        userSensorDriver?.let {
            UserDriverManager.getInstance().unregisterSensor(it.userSensor)
            adc.unregister()
        }
        userSensorDriver = null
    }

    fun setLowPowerMode(enabled: Boolean) {
        userSensorDriver?.setEnabled(!enabled)
    }

    private inner class CustomUserSensorDriver : UserSensorDriver {

        private val version = 1
        private var sensorType: Int = Sensor.REPORTING_MODE_CONTINUOUS
        var userSensor: UserSensor? = null
            get() {
                if (field == null) {
                    field = UserSensor.Builder()
                            .setCustomType(Sensor.TYPE_DEVICE_PRIVATE_BASE,
                                    "jp.lionas.androidthings.sensor.mcp3008",
                                    sensorType)
                            .setName(DRIVER_NAME)
                            .setVendor("")
                            .setVersion(version)
                            .setUuid(UUID.randomUUID())
                            .setDriver(this)
                            .build()
                }
                return field
            }

        @Throws(IOException::class)
        override fun read(): UserSensorReading {
            val intAdcArray = adc.readAdc(channels)
            val floatAdcArray = FloatArray(intAdcArray.size)
            for ((index, intAdc) in intAdcArray.withIndex()) {
                floatAdcArray[index] = intAdc.toFloat()
            }
            return UserSensorReading(floatAdcArray)
        }

        // for Power Management
        // Note: Sensors without low power modes can still use this callback to increase
        // or reduce the reporting frequency of the data in order to manage power consumption.
        override fun setEnabled(enabled: Boolean) {
            sensorType = if (enabled) {
                // normal
                Sensor.REPORTING_MODE_CONTINUOUS
            } else {
                // low power modes
                Sensor.REPORTING_MODE_ON_CHANGE
            }
        }

    }

}
