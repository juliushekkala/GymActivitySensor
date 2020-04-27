package com.iot2020.gymactivitysensor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sensor.*
import java.io.IOException
import java.util.*

class SensorActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var stepDetectorSensor: Sensor

    private lateinit var btDevice: BluetoothDevice

    private lateinit var btSocket: BluetoothSocket

    private var DEVICE_NAME = "LAPTOP-MT33NS2A"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

     var MY_UUID = UUID.fromString("d2e1c93a-887d-11ea-bc55-0242ac130003")


    private var REQUEST_ENABLE_BT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor)

        //Sensor stuff largely adapted from
        // https://stackoverflow.com/questions/51710147/kotlin-using-motion-sensor
        this.sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        //Linear acceleration sensor
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
            this.linearAccelerationSensor = it
        }

        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)?.let {
            this.stepDetectorSensor = it
        }
        //Bluetooth https://developer.android.com/guide/topics/connectivity/bluetooth.html

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            finish()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        var thread: ConnectThread
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.d("Socket", deviceName)
            Log.d("Socket", deviceHardwareAddress)
             if (deviceName == DEVICE_NAME) {
                 btDevice = device
                 thread = ConnectThread(btDevice)
                 thread.run()


            }
        }

        closeConnectionButton.setOnClickListener {
            cancel(btSocket)

        }

        sendMessageButton.setOnClickListener {
            val sendData = SendDataToServer(btSocket, "Test message".toByteArray(Charsets.UTF_8))
            sendData.run()
        }



    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //unnecessary
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //Adapted from https://stackoverflow.com/a/32803134
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION)  {
            //TODO: Send via bluetooth
            return

        } else if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            //TODO: Send via bluetooth
            return
        }
    }

    override fun onResume() {
        super.onResume()
      //  sensorManager.registerListener(
       //     this, this.linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
       // sensorManager.registerListener(
        //    this, this.stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter?.cancelDiscovery()


            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            mmSocket?.connect()

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            manageMyConnectedSocket(mmSocket!!)

        }

        fun manageMyConnectedSocket(socket: BluetoothSocket) {
            btSocket = socket
            Log.d("Socket", "Socket connected")
            return
        }
    }

    private inner class SendDataToServer(val socket: BluetoothSocket, val data: ByteArray): Thread() {




        override fun run() {

            Log.d("Socket", data.get(0).toString())
            Log.d("Socket", socket.isConnected.toString())
            try {
                val output = socket.outputStream
                output.flush()
                output.write(data)
                output.flush()
            } catch (e: IOException) {
                Log.e("Socket", "Error sending data", e)

            }

            return

        }

    }

    // Closes the client socket and causes the thread to finish.
    fun cancel(mmSocket: BluetoothSocket) {
        try {
            mmSocket?.close()
            Log.d("Socket", "closed connection")
        } catch (e: IOException) {
            Log.e("GymActivitySensor", "Could not close the client socket", e)
        }
    }


}