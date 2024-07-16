package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import classes.ObdEntry
import classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class DriveActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user_reference: DatabaseReference
    private lateinit var obd_reference: DatabaseReference
    private lateinit var mDatabase: DatabaseReference

    private lateinit var userId: String
    private lateinit var user: User

    private  var is_driving = false

    private lateinit var nameTv: TextView
//    private lateinit var carTypeTv: TextView
    private lateinit var statusTextView: TextView
    private lateinit var statsTextView: TextView

    private lateinit var drivebtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive)

        auth = FirebaseAuth.getInstance()
        user_reference = FirebaseDatabase.getInstance().reference.child("Users")
        obd_reference = FirebaseDatabase.getInstance().reference.child("Obd")
        mDatabase = FirebaseDatabase.getInstance().reference

        nameTv = findViewById(R.id.nametxt)
//        carTypeTv = findViewById(R.id.carTypetxt)
        statusTextView = findViewById(R.id.statusTextView)
        statsTextView = findViewById(R.id.stats)

        drivebtn = findViewById(R.id.driveButton)

        is_driving = false
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            fetchUserData(userId)
           // obdDisconnectionListener()
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserData(userId: String) {
        val userRef = user_reference.child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User::class.java) ?: return
                    nameTv.append("Hello " + user.getFullName())
//                    carTypeTv.append("Your car is  ${user.getCarType()} obd: ${user.getConnected_obd()}")
                    Toast.makeText(this@DriveActivity,"User data loaded", Toast.LENGTH_SHORT).show()
                    //Start listening for status updates from User and OBD
                    statusListener()
                    //Start listening for busy updates from OBD
                    obdBusyListener()
                } else {
                    Toast.makeText(this@DriveActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriveActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun driveScript(view: View?) {
        if(is_driving)
            obd_reference.child(user.getConnected_obd()).child("status").setValue("stop")
        else
            obd_reference.child(user.getConnected_obd()).child("status").setValue("start")

    }


    fun disconnectScript(view: View?) {
        if (user.getConnected_obd().isNotEmpty()) {
            obd_reference.child(user.getConnected_obd()).child("status").setValue("disconnected")
            obd_reference.child(user.getConnected_obd()).child("connected_uid").setValue("")
            obd_reference.child(user.getConnected_obd()).child("is_available").setValue(true)
            obd_reference.child(user.getConnected_obd()).child("is_connected").setValue(false)
            user.setStatus("disconnected")
            user.setConnectedObd("")
            user_reference.child(userId).setValue(user)
            startActivity(
                Intent(this@DriveActivity, MainActivity::class.java)
            )
            Toast.makeText(this, "Disconnected from OBD device" , Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Already disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    fun shutdownScript(view: View?) {
        //shut down the device
        if (user.getConnected_obd().isNotEmpty()) {
            obd_reference.child(user.getConnected_obd()).child("status").setValue("Shutting down")
            user.setConnectedObd("")
        }
        //disconnect user
        user.setStatus("disconnected")
        user_reference.child(userId).setValue(user)
        // move to main activity
        startActivity(
            Intent(this@DriveActivity, MainActivity::class.java)
        )
        Toast.makeText(this, "Shut down OBD device" , Toast.LENGTH_SHORT).show()
        finish()
    }

    fun statusListener(){
        //update user status
        user_reference.child(userId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status != null) {
                    user.setStatus(status)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriveActivity, "Failed to load status: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })

        //update OBD status
        obd_reference.child(user.getConnected_obd()).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val obdStatus = snapshot.getValue(String::class.java)
                if (obdStatus != null) {
                    val txt = "You are ${user.getStatus()}\nOBD ID: ${user.getConnected_obd()}"
                    statsTextView.text = txt
                    val statusTxt = "OBD status: $obdStatus"
                    updateStatus(obdStatus)
//                    statusTextView.text = statusTxt
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriveActivity, "Failed to load status: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun obdBusyListener(){
        //update start/stop switch
        obd_reference.child(user.getConnected_obd()).child("is_busy").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val busy = snapshot.getValue(Boolean::class.java)
                if (busy != null) {
                    if (!busy){
                        drivebtn.text = "Start"
                        drivebtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.start_icon, 0, 0, 0)
                        is_driving = false
                    }
                    else{
                        drivebtn.text = "Stop"
                        drivebtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.stop_icon, 0, 0, 0)
                        is_driving = true
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriveActivity, "Failed gettinf OBD busy data.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatus(status: String) {
        when {
            status.equals("start", ignoreCase = true) -> {
                statusTextView.text = status
                statusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.start_icon,
                    0,
                    0,
                    0
                )
            }

            status.equals("stop", ignoreCase = true) -> {
                statusTextView.text = status
                statusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.stop_icon,
                    0,
                    0,
                    0
                )
            }

            status.startsWith("error", ignoreCase = true) -> {
                statusTextView.text = status
                statusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.error,
                    0,
                    0,
                    0
                )
            }

            else -> {
                statusTextView.text = status
                statusTextView.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.baseline_autorenew_24,
                    0,
                    0,
                    0
                )
            }
        }
    }

}
