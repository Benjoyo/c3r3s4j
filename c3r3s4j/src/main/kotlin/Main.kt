import com.fazecast.jSerialComm.SerialPortIOException
import com.fazecast.jSerialComm.SerialPortInvalidPortException
import me.tongfei.progressbar.ProgressBar
import me.tongfei.progressbar.ProgressBarStyle
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.zip.CRC32
import kotlin.math.max

// Block size that is used if file isn't too large
private const val DEFAULT_BLOCK_SIZE = 256

/**
 * Main entry point, expects 2 arguments: name of the serial port and a file path to the image.
 */
fun main(args: Array<String>) {

    if (args.size != 2) {
        println("Arguments provided: ${args.size}. Expected: 2")
        println("Usage: SERIAL_PORT FILENAME")
        println("Example: /dev/tty1 kernel8.img")
        return
    }

    val serialPortDescriptor = args[0]
    val pathToImage = args[1]

    connectAndSendImage(serialPortDescriptor, pathToImage)
}

/**
 * Try to connect to c3r3s bootloader through serial port described by [serialPortDescriptor] and send the image at [pathToImage].
 */
fun connectAndSendImage(serialPortDescriptor: String, pathToImage: String) {

    val file = File(pathToImage)
    val bytes = try {
        file.readBytes()
    } catch (e1: FileNotFoundException) {
        println("Error: File not found!")
        return
    } catch (e2: IOException) {
        println("Error: Couldn't read file!")
        return
    }

    val serial = Serial(115200, serialPortDescriptor)
    try {
        serial.connect()
    } catch (e1: SerialPortInvalidPortException) {
        println("Bad serial port descriptor: $serialPortDescriptor")
        println("Usage: SERIAL_PORT FILENAME")
        println("Example: /dev/tty1 kernel8.img")
        return
    } catch (e2: SerialPortIOException) {
        println("Error: Failed to connect to serial port!")
        return
    }
    println("\n+ Successfully connected to $serialPortDescriptor!")

    // wait for the client to signal that it is ready
    serial.waitFor("c3r3s")
    println("+ Found c3r3s client! Sending 'boot' request...")

    // state that we want to send a new image
    serial.writeString("boot")

    // wait for the client to acknowledge
    serial.waitFor("lstn")
    println("+ Client answered request and is ready to receive image.")

    // init sending process
    serial.writeString("send")
    // tell starting address to copy image to (must match with linker script)
    serial.writeInt(0x80000)
    // tell size of whole image
    serial.writeInt(bytes.size)

    // send the image block by block, verifying for correct number of received bytes after each block
    sendImage(bytes, serial)

    println("\n+ Finished sending image, verifying checksum...")

    // send CRC32 checksum of whole image
    val crc32 = CRC32()
    crc32.update(bytes)
    serial.writeInt(crc32.value.toInt())

    // check CRC32 result from client
    val res = serial.readString("fail|good")
    when (res) {
        "good" -> {
            println("+++ Checksum OK +++")
        }
        "fail" -> {
            println("Error: Checksum doesn't match!")
        }
        else -> {
            println("Error: Unexpected answer from client.")
        }
    }
    println()

    serial.close()
}

/**
 * Partitions the [bytes] into at most 100 blocks and sends them one by one over [serial], verifying the number of received bytes after each block.
 */
private fun sendImage(bytes: ByteArray, serial: Serial) {
    val blockSize = max(DEFAULT_BLOCK_SIZE, (bytes.size / 100))
    var blocksRead = 0
    val blocks = mutableListOf<ByteArray>()

    // partition image into blocks of size blockSize
    while ((blocksRead * blockSize + blockSize) < bytes.size) {
        val block = bytes.sliceArray((blocksRead * blockSize) until (blocksRead * blockSize + blockSize))
        blocks.add(block)
        blocksRead += 1
    }

    // get last block, if necessary padded with zeros at the end to get full block
    val lastBlock = bytes.sliceArray((blocksRead * blockSize) until bytes.size).copyOf(blockSize)
    blocks.add(lastBlock)

    println("+ Start sending image...\n")

    // display a ASCII progress bar
    val progress = ProgressBar("", blocks.size.toLong(), ProgressBarStyle.ASCII)

    // send blocks one by one, in each step verifying that the client got everything
    blocks.forEachIndexed { i, block ->
        serial.writeInt(block.size)
        serial.writeBytes(block)
        Thread.sleep(25)
        // check if received bytes match with number of sent bytes so far
        val clientReceivedBytes = serial.readInt()
        if (clientReceivedBytes != ((i + 1) * block.size)) {
            println("Error: Client received only $clientReceivedBytes instead of ${block.size} bytes of the current block!")
            return
        }
        // increment progress bar
        progress.step()
    }

    // stop progress bar
    progress.close()
}



