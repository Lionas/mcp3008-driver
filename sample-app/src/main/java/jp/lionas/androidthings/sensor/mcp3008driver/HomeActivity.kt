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

package jp.lionas.androidthings.sensor.mcp3008driver

import android.app.Activity
import android.content.Context
import android.databinding.DataBindingUtil
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.DynamicSensorCallback
import android.os.Bundle
import android.os.Handler
import jp.lionas.androidthings.sensor.mcp3008.MCP3008Driver
import jp.lionas.androidthings.sensor.mcp3008driver.databinding.ActivityHomeBinding

/**
 * Analog to Digital Converter Driver Sample App
 * @author Naoki Seto(@Lionas)
 */
class HomeActivity : Activity(), SensorEventListener {

    companion object {
        const val DELAY_MIL_SECS = 60000L // 1min
    }

    private val callback = SensorCallback()
    private lateinit var sensorManager: SensorManager
    private lateinit var binding: ActivityHomeBinding
    private lateinit var driver: MCP3008Driver
    private val handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.data = SensorData(0)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerDynamicSensorCallback(callback)
        driver = MCP3008Driver(useSpi = true)
        driver.register()
    }

    override fun onDestroy() {
        sensorManager.unregisterDynamicSensorCallback(callback)
        driver.unregister()
        super.onDestroy()
    }

    private val diveIntoLowPowerMode: Runnable = Runnable {
        driver.setLowPowerMode(true)
    }

    private inner class SensorCallback : DynamicSensorCallback() {

        override fun onDynamicSensorConnected(sensor: Sensor) {
            if (sensor.type == Sensor.TYPE_DEVICE_PRIVATE_BASE &&
                    sensor.name!!.contentEquals(MCP3008Driver.DRIVER_NAME)) {
                sensorManager.registerListener(
                        this@HomeActivity,
                        sensor,
                        SensorManager.SENSOR_DELAY_NORMAL
                )

                // the mode is shifted to the low power mode
                handler.postDelayed(diveIntoLowPowerMode, DELAY_MIL_SECS)
            }
        }

        override fun onDynamicSensorDisconnected(sensor: Sensor?) {
            handler.removeCallbacks(diveIntoLowPowerMode)
            sensorManager.unregisterListener(this@HomeActivity)
            super.onDynamicSensorDisconnected(sensor)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            binding.data = SensorData(event.values[0].toInt())
        }
    }

}
