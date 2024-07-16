package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import classes.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var user_reference: DatabaseReference
    private lateinit var mDatabase: DatabaseReference

    private lateinit var user: User
    private lateinit var userId: String

    private lateinit var obdListener: ValueEventListener
    private var isConnectedToOBD = false

    private lateinit var connectBtn: Button

    private lateinit var nameTv: TextView
//    private lateinit var carTypeTv: TextView
    private lateinit var statusTextView: TextView

    private lateinit var obdSpinner: Spinner
    private lateinit var obdKeyEditText: EditText

    private lateinit var obdList: MutableList<String>
    private lateinit var obdMap: MutableMap<String, String>
    private lateinit var obdAdapter: ArrayAdapter<String>

    private lateinit var selectedObdId: String
    private lateinit var selectedObdName: String
    private lateinit var enteredObdKey: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDatabase = FirebaseDatabase.getInstance().reference
        nameTv = findViewById(R.id.nametxt)
//        carTypeTv = findViewById(R.id.carTypetxt)
        statusTextView = findViewById(R.id.statusTextView)
        obdSpinner = findViewById(R.id.obdSpinner)
        obdKeyEditText = findViewById(R.id.obdKeyEditText)

        obdList = mutableListOf()
        obdMap = mutableMapOf()
        obdAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, obdList)
        obdAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        obdSpinner.adapter = obdAdapter

        auth = FirebaseAuth.getInstance()
        user_reference = FirebaseDatabase.getInstance().reference.child("Users")

        fetchObdNames()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            fetchUserData(userId)
        } else {
            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
        }

        obdSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedObdName = obdList[position]
                selectedObdId = obdMap[selectedObdName] ?: ""
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedObdId = ""
            }
        }
    }

    private fun fetchObdNames() {
        val obdReference = FirebaseDatabase.getInstance().reference.child("Obd")
        obdReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                obdList.clear()
                obdMap.clear()
                for (obdSnapshot in snapshot.children) {
                    val obdId = obdSnapshot.key
                    val obdName = obdSnapshot.child("name").getValue(String::class.java)
                    val av = obdSnapshot.child("is_available").getValue(Boolean::class.java)
                    if (av == true && obdId != null && obdName != null) {
                        obdList.add(obdName)
                        obdMap[obdName] = obdId
                    }
                }
                obdAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load OBD names: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun fetchUserData(userId: String) {
        val userRef = user_reference.child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    user = snapshot.getValue(User::class.java) ?: return
                    nameTv.append("Hello " + user.getFullName())
                    //carTypeTv.append("Your car is  ${user.getCarType()}")
                    statusListener()
                    obdConnectionListener()
                    Toast.makeText(this@MainActivity,"User data loaded", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this@MainActivity, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun statusListener(){
        //status will be disconnected before starting connections.
        update_status("disconnected")
        //update user status
        user_reference.child(userId).child("status").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status != null) {
                    user.setStatus(status)
                    val txt = user.getStatus()
                    statusTextView.text = txt
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load status: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })

    }

    fun connectScript(view: View?) {
        enteredObdKey = obdKeyEditText.text.toString()

        if (selectedObdId.isNotEmpty() && enteredObdKey.isNotEmpty()) {
            val entry = ObdEntry(userId,selectedObdId, enteredObdKey)
            mDatabase.child("ObdEntries").child(selectedObdId).setValue(entry)
            update_status("pending for OBD $selectedObdName response")
            Toast.makeText(this, "Trying to connect to ${entry.getObdId()} $selectedObdName device" , Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please select an OBD device and enter the key", Toast.LENGTH_SHORT).show()
        }
    }


    private fun obdConnectionListener() {
        obdListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val obd = snapshot.getValue(String::class.java)
                if (obd != null) {
                    user.setConnectedObd(obd)
                    if (user.getConnected_obd() != "" && !isConnectedToOBD) {
                        isConnectedToOBD = true
                        startActivity(Intent(this@MainActivity, DriveActivity::class.java))
                        Toast.makeText(this@MainActivity, "Connected to OBD successfully!", Toast.LENGTH_SHORT).show()
                        user_reference.child(userId).child("connected_obd").removeEventListener(this)
                        finish()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load OBD data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        user_reference.child(userId).child("connected_obd").addValueEventListener(obdListener)
    }
    fun update_status(status: String){
        user.setStatus("disconnected")
        user_reference.child(userId).child("status").setValue(status)
    }
}
