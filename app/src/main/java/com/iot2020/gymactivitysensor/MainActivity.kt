package com.iot2020.gymactivitysensor

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_main)

        sensorActivityButton.setOnClickListener {
            val intent = Intent(applicationContext, SensorActivity::class.java)

            startActivity(intent)
        }
    }
}