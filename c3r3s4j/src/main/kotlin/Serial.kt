import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortIOException
import com.fazecast.jSerialComm.SerialPortInvalidPortException
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.regex.Pattern

/**
 *  This class provides simple platform-independent access to a given serial port described by [serialPortDescriptor] with a specific [baudRate].
 */
class Serial(val baudRate: Int, val serialPortDescriptor: String) {

    private lateinit var serialPort: SerialPort
    private lateinit var inputScanner: Scanner

    /**
     * Tries to connect to the given port at the given baud rate.
     * May throw [SerialPortInvalidPortException] or [SerialPortIOException] in case of failure.
     */
    fun connect() {
        // get port by string descriptor
        serialPort = SerialPort.getCommPort(serialPortDescriptor)
        // set port properties
        serialPort.baudRate = baudRate
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, Int.MAX_VALUE, Int.MAX_VALUE)
        // try to open port
        if (!serialPort.openPort()) {
            throw SerialPortIOException()
        }
        // set up scanner for easier stream reading
        inputScanner = Scanner(BufferedInputStream(serialPort.inputStream), Charsets.US_ASCII.name())
    }

    /**
     * Blocks until [text] appears in the byte stream (in US_ASCII).
     */
    fun waitFor(text: String) {
        assertInitialized()
        while (inputScanner.findWithinHorizon(text, 0) == null) {}
    }

    /**
     * Writes the given [text] to the serial output stream in US_ASCII characters.
     */
    fun writeString(text: String) {
        assertInitialized()
        serialPort.outputStream.write(text.toByteArray(Charsets.US_ASCII))
    }

    fun readString(pattern: String): String {
        assertInitialized()
        return inputScanner.findWithinHorizon(pattern, 0)
    }

    /**
     * Writes the given integer [value] to the serial output stream in little endian byte order.
     */
    fun writeInt(value: Int) {
        assertInitialized()
        serialPort.outputStream.write(value.toByteArrayLE())
    }

    /**
     * Reads a 4 byte integer in little endian byte order.
     */
    fun readInt(): Int {
        assertInitialized()
        val bytes = ByteArray(4)
        serialPort.inputStream.read(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    /**
     * Writes all bytes from the [byteArray] to the serial output stream.
     */
    fun writeBytes(byteArray: ByteArray) {
        assertInitialized()
        serialPort.outputStream.write(byteArray)
    }

    /**
     * Closes the serial port.
     */
    fun close() {
        serialPort.inputStream.close()
        serialPort.outputStream.close()
        serialPort.closePort()
    }

    /**
     * Private functions.
     */

    private fun Int.toByteArrayLE(): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array()
    }

    private fun assertInitialized() {
        if (!isInitialized()) throw IllegalAccessException("Serial not initialized. Call Serial.connect() first.")
    }

    private fun isInitialized() = ::serialPort.isInitialized && ::inputScanner.isInitialized
}