package com.example.cardriver

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import classes.*
import com.example.cardriver.StartActivity.Companion.SHARED_PREFS
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
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
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
                val tempList = mutableListOf<String>()

                for (obdSnapshot in snapshot.children) {
                    val obdId = obdSnapshot.key
                    var alive = false
                    var av = false
                    val obdName = obdSnapshot.child("name").getValue(String::class.java)
                    alive = obdSnapshot.child("is_alive").getValue(Boolean::class.java) == true
                    av = obdSnapshot.child("is_available").getValue(Boolean::class.java) == true
                    if (alive && av && obdId != null && obdName != null) {
                        if (true) {
                            obdList.add(obdName)  // Add to the beginning if in devices
                        } else {
                            tempList.add(obdName)  // Add to the temporary list if not in devices
                        }
                        obdMap[obdName] = obdId
                    }
                }

                obdList.addAll(tempList)  // Append the rest of the items
                obdAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load OBD names: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                    Toast.makeText(this@MainActivity, "User data loaded", Toast.LENGTH_SHORT).show()

                } else {
                    Toast.makeText(this@MainActivity, "User data not found", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    fun statusListener() {
        //status will be disconnected before starting connections.
        update_status("Disconnected")
        //update user status
        user_reference.child(userId).child("status")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val status = snapshot.getValue(String::class.java)
                    if (status != null) {
                        val wrongObd = extractIdFromStatus(status, "WRONGKEY")
                        if (wrongObd != null) {
                            user.removeDevice(wrongObd)
                            Toast.makeText(
                                this@MainActivity,
                                "Wrong key from $selectedObdName device",
                                Toast.LENGTH_SHORT
                            ).show()
                            update_status("Disconnected")
                        } else {
                            user.setStatus(status)
                            val txt = user.getStatus()
                            statusTextView.text = txt
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to load status: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            })

    }

    fun connectScript(view: View?) {
        if(obdList.isEmpty()) {
            Toast.makeText(
                this,
                "No available OBD device",
                Toast.LENGTH_SHORT
            ).show()
        }
        else if (selectedObdId.isNotEmpty()) {
            if (user.deviceExists(selectedObdId)) {
                val entry = user.getDeviceById(selectedObdId)
                mDatabase.child("ObdEntries").child(selectedObdId).setValue(entry)
                update_status("pending for OBD $selectedObdName response")
                Toast.makeText(
                    this,
                    "Trying to connect to $selectedObdName device",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val dialogView = layoutInflater.inflate(R.layout.dialog_obd_key, null)
                val dialogEditText = dialogView.findViewById<EditText>(R.id.dialogObdKeyEditText)
                val eyeimg = dialogView.findViewById<ImageView>(R.id.show_pass)
                var showPass = false
                // When the eye icon is clicked
                eyeimg.setOnClickListener {

                    if (showPass) {
                        dialogEditText.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        eyeimg.setImageResource(R.drawable.not_eye_icon)
                    } else {
                        dialogEditText.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        eyeimg.setImageResource(R.drawable.eye_icon)
                    }
                    dialogEditText.setSelection(dialogEditText.text.length) // Set the cursor to the end of the text
                    showPass = !showPass
                }
                AlertDialog.Builder(this)
                    .setTitle("Key for $selectedObdName OBD")
                    .setView(dialogView)
                    .setPositiveButton("OK") { dialog, _ ->
                        enteredObdKey = dialogEditText.text.toString()
                        if (enteredObdKey.isNotEmpty()) {
                            val entry = ObdEntry(userId, selectedObdId, enteredObdKey)
                            mDatabase.child("ObdEntries").child(selectedObdId).setValue(entry)
                            user.addDevice(entry)
                            update_status("pending for OBD $selectedObdName response")
                            Toast.makeText(
                                this,
                                "Trying to connect to $selectedObdName device",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this,
                                "Please enter the key",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
        else {
            Toast.makeText(
                this,
                "Please select an OBD device",
                Toast.LENGTH_SHORT
            ).show()
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
                        Toast.makeText(
                            this@MainActivity,
                            "Connected to OBD successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        user_reference.child(userId).child("connected_obd")
                            .removeEventListener(this)
                        finish()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to load OBD data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        user_reference.child(userId).child("connected_obd").addValueEventListener(obdListener)
    }

    fun update_status(status: String) {
        user.setStatus(status)
        user_reference.child(userId).setValue(user)
    }

    fun extractIdFromStatus(status: String, prefix: String): String? {
        val regex = Regex(""".*?:\s*WRONGKEY:(.*)""")
        val matchResult = regex.find(status)
        return matchResult?.groupValues?.get(1)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            //Edit Profile button
            R.id.action_edit_profile -> {
                startActivity(
                    Intent(this@MainActivity, EditProfileActivity::class.java)
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
                    Intent(this@MainActivity, StartActivity::class.java)
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
}