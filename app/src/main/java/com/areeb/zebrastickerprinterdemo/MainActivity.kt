package com.areeb.zebrastickerprinterdemo

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        private const val ACTION_USB_PERMISSION = "com.areeb.zebrastickerprinterdemo.USB_PERMISSION"
    }

    //
    private lateinit var usbManager: UsbManager
    private lateinit var usbDevice: UsbDevice
    private lateinit var endpoint: UsbEndpoint
    private lateinit var usbInterface: UsbInterface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        getVendorId()
        findZebraPrinter()
        btn.setOnClickListener {
            if (usbDevice != null && usbManager.hasPermission(usbDevice)) {
                startPrinting()
            } else {
                // Request permission if not granted
                requestUsbPermission()
            }
        }
    }


    private fun requestUsbPermission() {
        val permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_IMMUTABLE
        )
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionReceiver, filter)

        usbManager.requestPermission(usbDevice, permissionIntent)
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


                if (uintf.interfaceClass == UsbConstants.USB_CLASS_PRINTER) {
                    usbInterface = uintf
                    Log.e("vishal", "Printer USB interface found")
                    Toast.makeText(
                        this,
                        "connect to ${uintf.interfaceClass.toString()}",
                        Toast.LENGTH_SHORT
                    ).show()
//                    this.usbInterface = uintf

                    //find the endpooint
                    for (j in 0 until uintf.endpointCount) {


                        if(uintf.getEndpoint(j).type == UsbConstants.USB_ENDPOINT_XFER_BULK){
                            endpoint = uintf.getEndpoint(j)
                            break
                        }
                        Log.e(
                            "Areb",
                            "   Endpoint $j - Address: ${endpoint.address}, Type: ${endpoint.type}"
                        )
                    }
                    break
                }
            }
        } else {
            Toast.makeText(this, "Zebra printer is not connected", Toast.LENGTH_SHORT).show()
        }
    }

    // Call this function from where you want to start printing
    private fun startPrinting() {
        runBlocking {
            launch(Dispatchers.IO) {
                printToZebraPrinter()
            }
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

                connection.claimInterface(usbInterface, true)

                val cut_paper = byteArrayOf(0x1D, 0x56, 0x41, 0x10)

                connection.bulkTransfer(
                    endpoint, readZpl(zplFile),
                    zplFile.length().toInt(),
                    1000
                )

                connection.bulkTransfer(endpoint, cut_paper, cut_paper.size, 0)

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

    private val usbPermissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                if (device != null) {
                    // Check if the attached device is your Zebra printer
                    // Then handle permission request accordingly
                }
            } else if (intent?.action == ACTION_USB_PERMISSION) {
                Log.e(
                    "permosssAA",
                    intent.toString()
                )
                if (true || intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    // User has granted permission, proceed with further operations
                    startPrinting()
                } else {
                    // User has denied permission, handle it accordingly
                    Toast.makeText(
                        this@MainActivity,
                        "some error occur while permission",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "receiver error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(ACTION_USB_PERMISSION)
        registerReceiver(usbPermissionReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(usbPermissionReceiver)
    }


}