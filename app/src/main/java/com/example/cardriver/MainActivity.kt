package com.example.cardriver

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private lateinit var mDatabase: DatabaseReference
    private lateinit var nameEditText: EditText
    private lateinit var carTypeEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDatabase = FirebaseDatabase.getInstance().reference
        nameEditText = findViewById(R.id.nameEditText)
        carTypeEditText = findViewById(R.id.carTypeEditText)
    }

    fun startScript(view: View?) {
        val name = nameEditText.text.toString()
        val carType = carTypeEditText.text.toString()

        val commandData = mapOf(
            "start" to true,
            "name" to name,
            "carType" to carType
        )

        mDatabase.child("commands").setValue(commandData)
        mDatabase.child("commands").child("start").setValue(false) // Reset the command after sending
    }

    fun stopScript(view: View?) {
        mDatabase.child("commands").child("stop").setValue(true)
        mDatabase.child("commands").child("stop").setValue(false) // Reset the command after sending
    }

    fun shutdownScript(view: View?) {
        mDatabase.child("commands").child("shutdown").setValue(true)
        mDatabase.child("commands").child("shutdown").setValue(false)
    }
}
