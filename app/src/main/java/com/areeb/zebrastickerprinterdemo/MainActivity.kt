package com.areeb.zebrastickerprinterdemo

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    val btn by lazy {
        findViewById<Button>(R.id.btn)
    }

    companion object {
        private const val ZEBRA_PRINTER_VENDOR_ID = "2655"
        private const val ZEBRA_PRINTER_ID = "391"
    }

    //
    private lateinit var usbManager: UsbManager
    private lateinit var usbDevice: UsbDevice
    private lateinit var endpoint: UsbEndpoint
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        getVendorId()
        findZebraPrinter()
        btn.setOnClickListener {
            printToZebraPrinter()
        }
    }


    private fun getVendorId() {
        /**
         *  2655
        2.0
        Zebra Technologies
        ZTC ZD421-300dpi ZPL
        D6J234301344
        /dev/bus/usb/001/015
        2655
        2.0
        Zebra Technologies
        ZTC ZD421-300dpi ZPL
        D6J234301344
        39188**/

        for (device in usbManager.deviceList.values) {
            Log.e("dexx", device.deviceName)
            Log.e("dexx", device.vendorId.toString())
            Log.e("dexx", device.version)
            Log.e("dexx", device.manufacturerName.toString())
            Log.e("dexx", device.productName.toString())
            Log.e("dexx", device.serialNumber.toString())
            Log.e("dexx", device.productId.toString())
        }
    }

    private fun findZebraPrinter() {
        for (device in usbManager.deviceList.values) {
            if (device.vendorId == ZEBRA_PRINTER_VENDOR_ID.toInt() && device.productId == ZEBRA_PRINTER_ID.toInt()) {
                usbDevice = device
                break
            } else {
                Toast.makeText(this, "wrong vendor id ", Toast.LENGTH_SHORT).show()
            }
        }

        if (usbDevice != null) {
            usbDevice.interfaceCount
            for (i in 0 until usbDevice.interfaceCount) {
                val uintf = usbDevice.getInterface(i)
                Log.e(
                    "Areb",
                    "Interface $i - ${uintf.interfaceClass}, ${uintf.name}, ${uintf.endpointCount}"
                )
                for (j in 0 until uintf.endpointCount) {
                    endpoint = uintf.getEndpoint(j)
                    Log.e(
                        "Areb",
                        "   Endpoint $j - Address: ${endpoint.address}, Type: ${endpoint.type}"
                    )
                }

                if (uintf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    Log.e("vishal", "Printer USB interface found")
                    Toast.makeText(this, "connect to ${uintf.interfaceClass.toString()}", Toast.LENGTH_SHORT).show()
//                    this.usbInterface = uintf

                    break
                }
            }
        } else {
            Toast.makeText(this, "Zebra printer is not connected", Toast.LENGTH_SHORT).show()
        }
    }


    private fun printToZebraPrinter() {
        try {
            val connection: UsbDeviceConnection? = usbManager.openDevice(usbDevice)

            if (connection != null) {
                val zplFile = File(filesDir, "print_command.zpl")
                val outputStream = FileOutputStream(zplFile)
                outputStream.write("^XA^FO100,100^ADN,36,20^FDHello, Zebra!^FS^XZ".toByteArray())
                outputStream.close()

                connection.bulkTransfer(
                    endpoint, readZpl(zplFile),
                    zplFile.length().toInt(),
                    1000
                )

                connection.close()
            } else {
                Toast.makeText(
                    this,
                    "Failed to open connection to Zebra printer",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error printing to Zebra printer", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readZpl(file: File): ByteArray {
        val bytes = ByteArray(file.length().toInt())
        val fileInputStream = FileInputStream(file)
        fileInputStream.read(bytes)
        fileInputStream.close()
        return bytes
    }

}