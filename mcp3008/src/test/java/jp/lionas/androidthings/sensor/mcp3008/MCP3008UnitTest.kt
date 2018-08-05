package jp.lionas.androidthings.sensor.mcp3008

import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        val sensor = MCP3008()
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
        val sensor = MCP3008()
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

    @Test
    fun register() {
        val mockSensor = spy(MCP3008())
        mockSensor.setGpioPorts(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN, DEFAULT_PIN_MOS_OUT)
        mockSensor.peripheralManager = mockPeripheralManager

        doReturn(mockCsGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CS)
        doReturn(mockClockGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CLOCK)
        doReturn(mockMosInGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_IN)
        doReturn(mockMosOutGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_OUT)

        doNothing().`when`(mockCsGpio).setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        doNothing().`when`(mockClockGpio).setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        doNothing().`when`(mockMosInGpio).setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        doNothing().`when`(mockMosOutGpio).setDirection(Gpio.DIRECTION_IN)

        mockSensor.register()
    }

    @Test(expected = IOException::class)
    fun registerExpectedIOException() {
        val sensor = spy(MCP3008())
        sensor.setGpioPorts(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN, DEFAULT_PIN_MOS_OUT)
        sensor.peripheralManager = mockPeripheralManager

        doThrow(IOException()).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CS)
        doThrow(IOException()).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CLOCK)
        doThrow(IOException()).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_IN)
        doThrow(IOException()).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_OUT)

        sensor.register()
    }

    @Test(expected = NullPointerException::class)
    fun registerExpectedNullPointerException() {
        val sensor = spy(MCP3008())
        sensor.setGpioPorts(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN, DEFAULT_PIN_MOS_OUT)
        sensor.peripheralManager = mockPeripheralManager
        sensor.register()
    }

    @Test
    fun readAdc() {
        val mockSensor = spy(MCP3008())
        mockSensor.setGpioPorts(DEFAULT_PIN_CS, DEFAULT_PIN_CLOCK, DEFAULT_PIN_MOS_IN, DEFAULT_PIN_MOS_OUT)
        mockSensor.peripheralManager = mockPeripheralManager

        doReturn(mockCsGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CS)
        doReturn(mockClockGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_CLOCK)
        doReturn(mockMosInGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_IN)
        doReturn(mockMosOutGpio).`when`(mockPeripheralManager).openGpio(DEFAULT_PIN_MOS_OUT)
        doReturn(100).`when`(mockSensor).valueFromSelectedChannel

        mockSensor.register()
        val channels = intArrayOf(DEFAULT_CHANNEL)
        val value = mockSensor.readAdc(channels)

        verify(mockSensor, times(1)).initReadState()
        verify(mockSensor, times(1)).initChannelSelect(0)
        assertEquals(100, value[0])
    }

    @Test(expected = NullPointerException::class)
    fun readAdc_withoutRegister() {
        val mockSensor = spy(MCP3008())
        mockSensor.peripheralManager = mockPeripheralManager
        val channels = intArrayOf(DEFAULT_CHANNEL)
        mockSensor.readAdc(channels)
    }

}
