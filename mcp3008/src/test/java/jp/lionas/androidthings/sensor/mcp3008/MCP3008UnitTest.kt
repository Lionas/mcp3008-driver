package jp.lionas.androidthings.sensor.mcp3008

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import java.io.IOException

/**
 * Local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MCP3008UnitTest {

    @Mock
    private lateinit var mockCsGpio: Gpio

    @Mock
    private lateinit var mockClockGpio: Gpio

    @Mock
    private lateinit var mockMosInGpio: Gpio

    @Mock
    private lateinit var mockMosOutGpio: Gpio

    @Mock
    private lateinit var mockPeripheralManager: PeripheralManager

    @Rule
    @JvmField
    var mokitoRule = MockitoJUnit.rule()

    @Rule
    @JvmField
    var expectedException = ExpectedException.none()

    companion object {
        const val DEFAULT_PIN_CS = "GPIO1_IO10"
        const val DEFAULT_PIN_CLOCK = "GPIO6_IO13"
        const val DEFAULT_PIN_MOS_IN = "GPIO6_IO12"
        const val DEFAULT_PIN_MOS_OUT = "GPIO5_IO00"
        const val DEFAULT_CHANNEL = 0
    }

    @Test
    @Throws(IOException::class)
    fun close() {
        val sensor = MCP3008(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN,
                DEFAULT_PIN_MOS_OUT, DEFAULT_CHANNEL)
        sensor.mCsPin = mockCsGpio
        sensor.mClockPin = mockClockGpio
        sensor.mMosiPin = mockMosInGpio
        sensor.mMisoPin = mockMosOutGpio
        sensor.close()
        assertEquals(sensor.mCsPin, null)
        assertEquals(sensor.mClockPin, null)
        assertEquals(sensor.mMosiPin, null)
        assertEquals(sensor.mMisoPin, null)
    }

    @Test
    @Throws(IOException::class)
    fun close_safeToCallTwice() {
        val sensor = MCP3008(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN,
                DEFAULT_PIN_MOS_OUT, DEFAULT_CHANNEL)
        sensor.mCsPin = mockCsGpio
        sensor.mClockPin = mockClockGpio
        sensor.mMosiPin = mockMosInGpio
        sensor.mMisoPin = mockMosOutGpio
        sensor.close()
        sensor.close() // should not throw
        assertEquals(sensor.mCsPin, null)
        assertEquals(sensor.mClockPin, null)
        assertEquals(sensor.mMosiPin, null)
        assertEquals(sensor.mMisoPin, null)
    }

    @Test(expected = NullPointerException::class)
    fun registerExpectedNullPointerException() {
        val sensor = MCP3008(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN,
                DEFAULT_PIN_MOS_OUT, DEFAULT_CHANNEL)
        sensor.peripheralManager = mockPeripheralManager
        sensor.register()
    }

    @Test(expected = RuntimeException::class)
    fun registerExpectedRuntimeException() {
        val sensor = MCP3008(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN,
                DEFAULT_PIN_MOS_OUT, DEFAULT_CHANNEL)
        sensor.peripheralManager = null
        sensor.register()
    }

    @Test
    fun readAdc() {
        val sensor = MCP3008(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN,
                DEFAULT_PIN_MOS_OUT, DEFAULT_CHANNEL)
        sensor.peripheralManager = mockPeripheralManager
        sensor.mCsPin = mockCsGpio
        sensor.mClockPin = mockClockGpio
        sensor.mMosiPin = mockMosInGpio
        sensor.mMisoPin = mockMosOutGpio
        val value = sensor.readAdc()
        assertEquals(0, value)
    }

}
