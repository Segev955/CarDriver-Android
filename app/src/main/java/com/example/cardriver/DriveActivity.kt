package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.Visibility
import classes.ObdEntry
import classes.User
import com.example.cardriver.StartActivity.Companion.SHARED_PREFS
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
    private lateinit var speedLimiteTv: TextView
    private lateinit var statusTextView: TextView
    private lateinit var statsTextView: TextView

    private lateinit var locationimg: ImageView

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
        speedLimiteTv = findViewById(R.id.speedLimitTextView)

        locationimg = findViewById(R.id.locationIcon)

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
            obd_reference.child(user.getConnected_obd()).child("status").setValue("Disconnect")
           // obd_reference.child(user.getConnected_obd()).child("connected_uid").setValue("")
          //  obd_reference.child(user.getConnected_obd()).child("is_available").setValue(true)
          //  obd_reference.child(user.getConnected_obd()).child("is_connected").setValue(false)
            user.setStatus("Disconnecting")
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
        user.setStatus("Disconnected")
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
                    val txt = "${user.getStatus()}\nOBD ID: ${user.getConnected_obd()}"
                    statsTextView.text = txt
                    val statusTxt = "OBD status: $obdStatus"
//                    updateStatus(obdStatus)
                    statusTextView.text = statusTxt
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DriveActivity, "Failed to load status: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    //update speed limit
    obd_reference.child(user.getConnected_obd()).child("speed_limit").addValueEventListener(object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val speed_limit = snapshot.getValue(String::class.java)
            if (speed_limit != null) {
                if(speed_limit.toFloat() <= 0){
                    locationimg.setImageResource(R.drawable.red_location_icon)
                    speedLimiteTv.visibility = View.GONE
                }
                else {
                    locationimg.setImageResource(R.drawable.green_location_icon)
                    speedLimiteTv.text = speed_limit.toString()
                    speedLimiteTv.visibility = View.VISIBLE
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(this@DriveActivity, "Failed to load speed limit: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    })
}

    //update start/stop button
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            //Edit Profile button
            R.id.action_edit_profile -> {
                startActivity(
                    Intent(this@DriveActivity, EditProfileActivity::class.java)
                )
                Toast.makeText(this, "Edit Profile clicked", Toast.LENGTH_SHORT).show()
                true
            }
            //LogOut button
            R.id.action_logout -> {
                val sharedPreferences = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("remember", "false")
                editor.apply()
                FirebaseAuth.getInstance().signOut()
                startActivity(
                    Intent(this@DriveActivity, StartActivity::class.java)
                )
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.action_keep_screen_on -> {
                // Handle Keep Screen On toggle
                item.isChecked = !item.isChecked
                val windowLayoutParams = window.attributes
                if (item.isChecked) {
                    windowLayoutParams.flags = windowLayoutParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                } else {
                    windowLayoutParams.flags = windowLayoutParams.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
                }
                window.attributes = windowLayoutParams
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /*private fun updateStatus(status: String) {
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
    }*/

}
