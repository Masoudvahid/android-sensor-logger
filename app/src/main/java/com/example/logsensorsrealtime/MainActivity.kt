package com.example.logsensorsrealtime

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import java.net.Socket
import java.io.OutputStream
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.LinkedList
import java.util.Queue

import android.content.SharedPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null
    private var linearAccelerationSensor: Sensor? = null
    private lateinit var hostInput: EditText
    private lateinit var portInput: EditText
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request permission for accessing sensors and internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }

        // Connect to TCP server
        connectToTCPServer()

        // Initialize sensor manager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        // Register sensor listener
        sensorManager.registerListener(
            rotationVectorSensorListener,
            rotationVectorSensor,
            SensorManager.SENSOR_DELAY_NORMAL
//            SensorManager.SENSOR_DELAY_FASTEST
        )
        // Register sensor listener
        sensorManager.registerListener(
            linearAccelerationSensorListener,
            linearAccelerationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
//            SensorManager.SENSOR_DELAY_FASTEST
        )

        loadTextbox()
    }

    fun saveTexbox() {
        val insetedText = hostInput.text.toString()

        val sharedPreferences = getSharedPreferences("sharePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.apply{putString("STRING_KEY", insetedText)}.apply()
    }

    private fun loadTextbox(){
        val sharedPreferences = getSharedPreferences("sharePrefs", Context.MODE_PRIVATE)
        val savedString = sharedPreferences.getString("STRING_KEY", "192.169.0.0")
        hostInput.setText(savedString)
    }

    fun connectButton(view: View?) {
        saveTexbox()
        connectToTCPServer()
    }

    fun disconnectButton(view: View?) {
        disconnectFromTCPServer()
    }

    private val rotationVectorSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == rotationVectorSensor) {
                val rotationVectorData = event.values
                sendDataToServer("rotationVectorData", rotationVectorData, event.timestamp)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }

    private val linearAccelerationSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor == linearAccelerationSensor) {
                val linearAccelerationData = event.values
                sendDataToServer("linearAccelerationData", linearAccelerationData, event.timestamp)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }

/*
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            var rotationVectorData: FloatArray? = null
            var linearAccelerationData: FloatArray? = null
            if (event.sensor == rotationVectorSensor) {
                rotationVectorData = event.values
//                println("Rotation Vector ---->    " + rotationVectorData.joinToString(","))
            } else if (event.sensor == linearAccelerationSensor) {
                linearAccelerationData = event.values
//                println("Linear Acceleration ---->    " + linearAccelerationData.joinToString(","))
            }

            sendDataToServer(rotationVectorData, linearAccelerationData)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        }
    }
*/


    // Function to establish TCP connection
    @OptIn(DelicateCoroutinesApi::class)
    private fun connectToTCPServer() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                hostInput = findViewById(R.id.hostText)
                portInput = findViewById(R.id.portText)

                val serverAddress = hostInput.text.toString()
                val serverPort = portInput.text.toString().toInt()

                socket = Socket(serverAddress, serverPort)
                outputStream = socket?.getOutputStream()
            } catch (_: Exception) {
            }
        }
    }

    // Function to establish TCP connection
    private fun disconnectFromTCPServer() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: Exception) {
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendDataToServer(sensorName: String, data: FloatArray, timestamp: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                // Convert sensor data to a string and send it to the server
                val dataString = encodeSensorData(sensorName, data, timestamp)
                val byteArray = dataString.toByteArray()

                outputStream?.write(byteArray)
            } catch (_: Exception) {
            }
        }
    }

    private fun encodeSensorData(sensorName: String, data: FloatArray, timestamp: Long): String {
        val json = JSONObject()
        json.put( sensorName, JSONArray(data.toList()))
        json.put("timestamp", timestamp)
//        json.put("timestamp", Instant.ofEpochMilli(timestamp / 1000000).toString())
        println(json)
        return json.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister sensor listener and release resources
        sensorManager.unregisterListener(linearAccelerationSensorListener)
        sensorManager.unregisterListener(rotationVectorSensorListener)


    }




}