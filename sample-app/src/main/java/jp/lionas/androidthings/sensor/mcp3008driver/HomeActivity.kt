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
import android.util.Log
import com.google.android.things.contrib.driver.button.Button
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay
import com.google.android.things.contrib.driver.ht16k33.Ht16k33
import com.google.android.things.contrib.driver.pwmspeaker.Speaker
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat
import com.google.android.things.pio.Gpio
import jp.lionas.androidthings.sensor.mcp3008.MCP3008Driver
import jp.lionas.androidthings.sensor.mcp3008driver.databinding.ActivityHomeBinding

/**
 * Analog to Digital Converter Driver Sample App
 * Musical instrument using Rainbow HAT
 * @author Naoki Seto(@Lionas)
 */
class HomeActivity : Activity(), SensorEventListener {

    companion object {
        const val DELAY_MIL_SECS = 60000L // 1min
        const val C = 262
        const val D = 294
        const val E = 330
        const val F = 349
        const val G = 392
        const val A = 440
        const val B = 494
    }

    private val callback = SensorCallback()
    private lateinit var sensorManager: SensorManager
    private lateinit var binding: ActivityHomeBinding
    private lateinit var driver: MCP3008Driver
    private val handler = Handler()

    private lateinit var speaker: Speaker
    private val sounds = listOf(C, D, E, F, G, A, B)
    private val soundsStr = listOf("C", "D", "E", "F", "G", "A", "B")
    private var prevSounds: Int = 0

    private var currentStep: Int = 0
    private lateinit var buttonA: Button
    private lateinit var buttonB: Button
    private var isPressedB: Boolean = false

    private lateinit var ledRed: Gpio
    private lateinit var ledBlue: Gpio
    private lateinit var ledGreen: Gpio
    private lateinit var segment: AlphanumericDisplay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        binding.data = SensorData(soundsStr[prevSounds])
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.registerDynamicSensorCallback(callback)
        driver = MCP3008Driver(useSpi = true, channels = intArrayOf(0, 1))
        driver.register()
        ledRed = RainbowHat.openLedRed()
        ledBlue = RainbowHat.openLedBlue()
        ledGreen = RainbowHat.openLedGreen()
        speaker = RainbowHat.openPiezo()
        buttonA = RainbowHat.openButtonA()
        buttonA.setOnButtonEventListener { _, pressed ->
            ledRed.value = pressed
            if (pressed) speaker.stop()
        }

        buttonB = RainbowHat.openButtonB()
        buttonB.setOnButtonEventListener { _, pressed ->
            ledGreen.value = pressed
            if (!isPressedB and pressed) {
                isPressedB = true
                speaker.play(sounds[currentStep].toDouble())
            }
            else if (isPressedB and !pressed) {
                isPressedB = false
                speaker.stop()
            }
        }

        segment = RainbowHat.openDisplay()
        segment.setBrightness(Ht16k33.HT16K33_BRIGHTNESS_MAX)
        segment.display(soundsStr[currentStep])
        segment.setEnabled(true)
    }

    override fun onDestroy() {
        speaker.stop()
        segment.clear()
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
            val value = event.values[0].toInt() + event.values[1].toInt()
            currentStep = value / 170
            if (isPressedB and(prevSounds != currentStep)) {
                speaker.stop()
                speaker.play(sounds[currentStep].toDouble())
                prevSounds = currentStep
            }
            binding.data = SensorData(soundsStr[currentStep])
            segment.display(soundsStr[currentStep])
        }
    }

}
