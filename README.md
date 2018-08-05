MCP3008 driver for Android Things
================================

This driver supports 8-Channel 10-Bit Analog to Digital Converters ([MCP3008][product_mcp3008]).

NOTE: these drivers are not production-ready. They are offered as sample
implementations of Android Things user space drivers for common peripherals. 
There is no guarantee of correctness, completeness or robustness.

How to use the driver
---------------------

### Gradle dependency

To use the `mcp3008` driver, simply add the line below to your project's `build.gradle`,
where `<version>` matches the last version of the driver available on jcenter.

```
dependencies {
    compile 'jp.lionas.androidthings.sensor:mcp3008:<version>'
}
```

### Sample usage

```kotlin
import jp.lionas.androidthings.sensor.mcp3008.MCP3008Driver

// Access the dust sensor:

private val mcp3008: MCP3008 = MCP3008()

mcp3008.register()

// Read the current values:

mcp3008.readAdc(channels)

// Close the dust sensor when finished:

mcp3008.unregister()
```

If you need to read values continuously, you can register the mcp3008 with the system and
listen for values using the [Sensor APIs][sensors]:
```kotlin
// Register the driver:

private val callback = SensorCallback()
private lateinit var sensorManager: SensorManager
private lateinit var driver: MCP3008Driver

sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
sensorManager.registerDynamicSensorCallback(callback)
driver = MCP3008Driver()
driver.register()

private inner class SensorCallback : DynamicSensorCallback() {

    override fun onDynamicSensorConnected(sensor: Sensor) {
        if (sensor.type == Sensor.TYPE_DEVICE_PRIVATE_BASE &&
                sensor.name!!.contentEquals(MCP3008Driver.DRIVER_NAME)) {
            sensorManager.registerListener(
                    this@HomeActivity,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            )
        }
    }

    override fun onDynamicSensorDisconnected(sensor: Sensor?) {
        sensorManager.unregisterListener(this@HomeActivity)
        super.onDynamicSensorDisconnected(sensor)
    }

}

override fun onSensorChanged(event: SensorEvent?) {
    // digital value = event.values[0] (0 to 1023)
}

// Unregister and close the driver when finished:

sensorManager.unregisterDynamicSensorCallback(callback)
sensorDriver.unregister()
```

License
-------

Copyright 2018 Naoki Seto (@Lionas).

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.

[product_mcp3008]: http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf
[sensors]: https://developer.android.com/guide/topics/sensors/sensors_overview.html
