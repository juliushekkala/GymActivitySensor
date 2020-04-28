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
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sensor.*
import java.io.IOException
import java.util.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

class SensorActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var stepDetectorSensor: Sensor

    private lateinit var btDevice: BluetoothDevice

    private lateinit var btSocket: BluetoothSocket

    private var DEVICE_NAME = "LAPTOP-MT33NS2A"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    var MY_UUID = UUID.fromString("d2e1c93a-887d-11ea-bc55-0242ac130003")

    var stepCounter = 0

    var accelerationList = mutableListOf<Float>()

    var lastSendTime = System.currentTimeMillis()

    private var connectedToServer = false




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

        closeConnectionButton.setOnClickListener {
            cancel(btSocket)

        }

        startConnectionButton.setOnClickListener {
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
            pairedDevices?.forEach loop@{ device ->
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                if (deviceName == DEVICE_NAME) {
                    btDevice = device
                    thread = ConnectThread(btDevice)
                    thread.run()
                    return@loop
                }
            }

            //Init time variable
            lastSendTime = System.currentTimeMillis()
        }



    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //unnecessary
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!connectedToServer) {
            return
        }
        //Adapted from https://stackoverflow.com/a/32803134
        if (event?.sensor?.type == Sensor.TYPE_LINEAR_ACCELERATION)  {
            //Calculate acceleration
            var acceleration = sqrt(
                event.values[0].pow(2) +
                event.values[1].pow(2) +
                event.values[2].pow(2))
            //Add to the list
            accelerationList.add(acceleration)
            val currentTime = System.currentTimeMillis()
            //If over 1 second from last time data was sent, send data to server
            if (currentTime - lastSendTime > 1000) {
                //average of the acceleration values since last time sent
                val accelerationAvr = accelerationList.average()
                //Not as much accuracy is needed
                val roundedAccelerationAvr = round(accelerationAvr * 1000) / 1000
                //Payload
                val data = ("$roundedAccelerationAvr,$stepCounter").toByteArray(Charsets.UTF_8)
                var dataSend = SendDataToServer(btSocket, data)
                dataSend.run()
                accelerationList.clear()
                stepCounter = 0
            }


            return

            //If step detector, add 1 step to the counter
        } else if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            stepCounter += 1
            return
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            this, this.linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(
            this, this.stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
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
            try {
                mmSocket?.connect()
                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                manageMyConnectedSocket(mmSocket!!)
            } catch (e: IOException) {
                runOnUiThread(Runnable {
                    Toast.makeText(
                        applicationContext, "Could not connect to the Bluetooth server",
                        Toast.LENGTH_LONG
                    ).show()
                })
            }
        }






        fun manageMyConnectedSocket(socket: BluetoothSocket) {
            btSocket = socket
            Log.d("Socket", "Socket connected")
            runOnUiThread(Runnable {
                Toast.makeText(
                    applicationContext, "Connected to the Bluetooth server",
                    Toast.LENGTH_LONG
                ).show()
            })
            connectedToServer = true
            return
        }
    }

    private inner class SendDataToServer(val socket: BluetoothSocket, val data: ByteArray): Thread() {




        override fun run() {

            try {
                val output = socket.outputStream
                output.write(data)
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
            Toast.makeText(applicationContext, "Connnection was closed", Toast.LENGTH_LONG).show()
            connectedToServer = false
        } catch (e: IOException) {
            Log.e("GymActivitySensor", "Could not close the client socket", e)
        }
    }


}