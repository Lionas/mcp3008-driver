package jp.lionas.androidthings.sensor.mcp3008

import com.google.android.things.pio.SpiDevice
import com.google.android.things.pio.PeripheralManager
import com.nhaarman.mockito_kotlin.*
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import java.io.IOException

/**
 * Local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class MCP3008UnitTest {

    @Mock
    private lateinit var mockSpiDevice: SpiDevice

    @Mock
    private lateinit var mockPeripheralManager: PeripheralManager

    @Rule
    @JvmField
    var mokitoRule: MockitoRule = MockitoJUnit.rule()

    @Rule
    @JvmField
    var expectedException: ExpectedException = ExpectedException.none()

    companion object {
        const val DEFAULT_SPI_NAME = "SPI3.0"
    }

    @Test
    fun register() {
        val mockSensor = spy(MCP3008())
        mockSensor.setSpi(DEFAULT_SPI_NAME)
        mockSensor.peripheralManager = mockPeripheralManager

        doReturn(mockSpiDevice).`when`(mockPeripheralManager).openSpiDevice(DEFAULT_SPI_NAME)

        mockSensor.register()
    }

    @Test(expected = IOException::class)
    fun registerExpectedIOException() {
        val sensor = spy(MCP3008())
        sensor.setSpi(DEFAULT_SPI_NAME)
        sensor.peripheralManager = mockPeripheralManager

        doThrow(IOException()).`when`(mockPeripheralManager).openSpiDevice(DEFAULT_SPI_NAME)

        sensor.register()
    }

    @Test
    fun createTransferData() {
        val mockSensor = spy(MCP3008())

        val expectTransferData = ByteArray(3)
        expectTransferData[0] = 1
        expectTransferData[1] = 128.toByte()
        expectTransferData[2] = 0

        val actualTransferData = mockSensor.createTransferData(0)

        assertEquals(expectTransferData[0], actualTransferData[0])
        assertEquals(expectTransferData[1], actualTransferData[1])
        assertEquals(expectTransferData[2], actualTransferData[2])
    }

    @Test
    fun extractReceivedData() {
        val mockSensor = spy(MCP3008())

        val receivedData = ByteArray(3)
        receivedData[0] = 0
        receivedData[1] = 3
        receivedData[2] = 128.toByte()

        assertEquals(896, mockSensor.extractReceivedData(receivedData))
    }

    @Test
    fun readAdc() {
        val mockSensor = spy(MCP3008())
        mockSensor.setSpi(DEFAULT_SPI_NAME)
        mockSensor.peripheralManager = mockPeripheralManager

        doReturn(mockSpiDevice).`when`(mockPeripheralManager).openSpiDevice(DEFAULT_SPI_NAME)
        doNothing().`when`(mockSpiDevice).setMode(SpiDevice.MODE0)
        doNothing().`when`(mockSpiDevice).setFrequency(1350000)
        doNothing().`when`(mockSpiDevice).setBitsPerWord(8)
        doNothing().`when`(mockSpiDevice).setBitJustification(SpiDevice.BIT_JUSTIFICATION_MSB_FIRST)

        mockSensor.register()

        val channels = intArrayOf(0, 1)
        val actualValue: IntArray = mockSensor.readAdc(channels)

        verify(mockSensor, times(1)).createTransferData(0)
        verify(mockSensor, times(1)).createTransferData(1)
        verify(mockSpiDevice, times(2)).transfer(any(), any(), any())
        verify(mockSensor, times(2)).extractReceivedData(any())

        assertEquals(2, actualValue.size)
        assertEquals(0, actualValue[0])
        assertEquals(0, actualValue[1])
    }

    @Test
    @Throws(IOException::class)
    fun unregister() {
        val mockSensor = spy(MCP3008())
        mockSensor.peripheralManager = mockPeripheralManager
        mockSensor.setSpi(DEFAULT_SPI_NAME)
        doReturn(mockSpiDevice).`when`(mockPeripheralManager).openSpiDevice(DEFAULT_SPI_NAME)
        mockSensor.register()
        mockSensor.unregister()
        verify(mockSpiDevice, times(1)).close()
    }

    @Test
    @Throws(IOException::class)
    fun close() {
        val mockSensor = spy(MCP3008())
        mockSensor.close()
        verify(mockSensor, times(1)).unregister()
    }

    @Test
    @Throws(IOException::class)
    fun close_safeToCallTwice() {
        val mockSensor = spy(MCP3008())
        mockSensor.close()
        mockSensor.close() // should not throw
        verify(mockSensor, times(2)).unregister()
    }

}
