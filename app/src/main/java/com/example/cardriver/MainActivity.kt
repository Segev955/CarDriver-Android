package com.example.cardriver

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import classes.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user_reference: DatabaseReference
    private lateinit var mDatabase: DatabaseReference

    private lateinit var connectBtn: Button

    private lateinit var nameTv: TextView
    private lateinit var carTypeTv: TextView

    private lateinit  var user: User
    private lateinit  var userId: String

    private lateinit var statusTextView: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDatabase = FirebaseDatabase.getInstance().reference
        nameTv = findViewById(R.id.nametxt)

        carTypeTv = findViewById(R.id.carTypetxt)

        auth = FirebaseAuth.getInstance()
        user_reference = FirebaseDatabase.getInstance().reference.child("Users")

        statusTextView = findViewById(R.id.statusTextView)



        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            fetchUserData(userId)
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
        }

    }

    private fun fetchUserData(userId: String) {
        val userRef = user_reference.child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fullName = snapshot.child("fullName").getValue(String::class.java)
                    val email = snapshot.child("email").getValue(String::class.java)
                    val date = snapshot.child("date").getValue(String::class.java)
                    val gender = snapshot.child("gender").getValue(String::class.java)
                    val carType = snapshot.child("carType").getValue(String::class.java)

                    user = User(fullName, email, date, gender, carType)

                    // You can now use the user object
                    nameTv.append("Hello "+user.getFullName())
                    carTypeTv.append("Your car is "+user.getCarType())
                    Toast.makeText(this@MainActivity, "User data loaded", Toast.LENGTH_SHORT).show()
                    //Start listening for status updates
                    statusListener()
                } else {
                    Toast.makeText(this@MainActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun connectScript(view: View?) {
        mDatabase.child("request").setValue(userId)

    }
    fun startScript(view: View?) {
        val carType = user.getCarType()
        val commandData = mapOf(
            "start" to true,
            "uid" to userId,
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

    fun statusListener(){
        user_reference.child(userId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status != null) {
                    user.updateStatus(status)
                    statusTextView.text = user.getStatus()
                }


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load status: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
}
