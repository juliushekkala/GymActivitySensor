package com.iot2020.gymactivitysensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity

class SensorActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var linearAccelerationSensor: Sensor
    private lateinit var stepDetectorSensor: Sensor

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
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
        sensorManager.registerListener(
            this, this.linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(
            this, this.stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}