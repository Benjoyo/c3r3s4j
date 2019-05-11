# c3r3s4j
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

This is a Kotlin (JVM) implementation of a command-line serial bootloader server for the Raspberry Pi 3 using the c3r3s bootloader.

## c3r3s
[c3r3s](https://github.com/robey/c3r3s) is an awesome little bootloader (client) for the Raspberry Pi 3 that chain-loads 
bare-metal programs or operating system kernels to the Raspi over UART. This makes it possible to iteratively deploy your software
to the Raspi without moving the SD card in and out all the time. 

## c3r3s4j
c3r3s4j is just a Kotlin alternative to the original Rust command-line tool that is used to send the kernel over serial. 
It works platform independent on the JVM.

## How to use

1. Install the c3r3s bootloader on your Raspberry's SD card like described [here](https://github.com/robey/c3r3s#how-to-use)
1. Download the latest executable .jar-file from `releases`
2. On Linux, run:

   ```
   sudo java -jar c3r3s4j.jar /dev/ttyX kernel8.img
   ```
   Or on Windows:
   ```
   java -jar c3r3s4j.jar COMX kernel8.img
   ```
   
## What it does   

The program will connect to the port, wait for the bootloader to respond and will then initiate the block-wise transfer of the given kernel image.
After the full transfer the data is CRC32-checked. Finally, the bootloader will execute your kernel code and you can connect to the Pi using a serial console.  

A successful output looks like this:

```
+ Successfully connected to COM3!
+ Found c3r3s client! Sending 'boot' request...
+ Client answered request and is ready to receive image.
+ Start sending image...

 100% [=============================================] 3/3 (0:00:01 / 0:00:00)

+ Finished sending image, verifying checksum...
+++ Checksum OK +++
```

This setup allows for a workflow like this:
2. Reboot Raspi (by cutting power or better via a command in your Raspi-OS cli)
1. Build kernel image
3. Run c3r3s4j
4. Open serial console
5. Repeat

Steps 2.-4. can even be combined to a single command, making everything even more comfortable.
