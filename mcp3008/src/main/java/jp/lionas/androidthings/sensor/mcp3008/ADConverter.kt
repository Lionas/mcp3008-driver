package jp.lionas.androidthings.sensor.mcp3008

interface ADConverter {
    fun register()
    fun readAdc(): Int
    fun close()
    fun unregister()
}
