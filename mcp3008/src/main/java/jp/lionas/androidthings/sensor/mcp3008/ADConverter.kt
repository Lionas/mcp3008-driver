package jp.lionas.androidthings.sensor.mcp3008

interface ADConverter {
    fun register()
    fun readAdc(channels: IntArray): IntArray
    fun close()
    fun unregister()
}
