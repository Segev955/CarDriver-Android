import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import classes.ObdEntry
import classes.User
import com.example.cardriver.MapActivity
import com.example.cardriver.R

class DeviceAdapter(private val context: Context, private val devices: List<ObdEntry>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val obdRef: DatabaseReference = database.getReference("Obd")
    private val usersRef: DatabaseReference = database.getReference("Users") // Reference to users

    companion object {
        private const val STOLEN = "STOLEN"
        private const val NEVER_CHECKED = "Never Checked"
        private const val UNKNOWN_DEVICE = "Unknown Device"
    }

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
        val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)

        var latitude: Double = 0.0
        var longitude: Double = 0.0
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        val obdId = device.getObd_id()

        if (obdId != null) {
            fetchDeviceData(obdId, holder)
        }

        holder.itemView.setOnClickListener {
            showDeviceDialog(device)
        }

        // Add a click listener to open the map when the location is clicked
        holder.locationTextView.setOnClickListener {
            val intent = Intent(context, MapActivity::class.java)
            intent.putExtra("LATITUDE", holder.latitude)
            intent.putExtra("LONGITUDE", holder.longitude)
            context.startActivity(intent)
        }

    }


    private fun openMap(latitude: Double, longitude: Double) {
        val geoUri = "geo:$latitude,$longitude?q=$latitude,$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
        intent.setPackage("com.google.android.apps.maps") // You can remove this to allow other map apps
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "No map application found", Toast.LENGTH_SHORT).show()
        }
    }




    override fun getItemCount(): Int = devices.size

    // Fetch OBD data and display
    private fun fetchDeviceData(obdId: String, holder: DeviceViewHolder) {
        obdRef.child(obdId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val obdName = dataSnapshot.child("name").value as? String ?: UNKNOWN_DEVICE
                    val lastDriver = dataSnapshot.child("last_driver").value as? String ?: NEVER_CHECKED
                    val latitude = dataSnapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val longitude = dataSnapshot.child("longitude").getValue(Double::class.java) ?: 0.0

                    displayObdNameWithDriver(obdName, lastDriver, holder)

                    // Store the latitude and longitude in the holder for later use
                    holder.latitude = latitude
                    holder.longitude = longitude

                    // Display the location in the locationTextView
                    holder.locationTextView.text = "Last Location: $latitude, $longitude"
                } else {
                    holder.deviceNameTextView.text = UNKNOWN_DEVICE
                    holder.locationTextView.text = "Last Location: Unknown"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                holder.deviceNameTextView.text = "Error fetching name"
                holder.locationTextView.text = "Last Location: Error"
            }
        })
    }



    // Display OBD name with driver information
    private fun displayObdNameWithDriver(obdName: String, lastDriver: String, holder: DeviceViewHolder) {
        when (lastDriver) {
            STOLEN -> displayStolenDevice(obdName, holder)
            NEVER_CHECKED -> holder.deviceNameTextView.text = formatDeviceNameWithDriver(obdName, lastDriver, false)
            else -> fetchUserName(lastDriver, obdName, holder)
        }
    }

    // Highlight "STOLEN" in red and bold
    private fun displayStolenDevice(obdName: String, holder: DeviceViewHolder) {
        val deviceDisplay = SpannableString("$obdName ($STOLEN)")
        val stolenIndex = deviceDisplay.indexOf(STOLEN)
        deviceDisplay.setSpan(StyleSpan(android.graphics.Typeface.BOLD), stolenIndex, stolenIndex + STOLEN.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        deviceDisplay.setSpan(ForegroundColorSpan(Color.RED), stolenIndex, stolenIndex + STOLEN.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        holder.deviceNameTextView.text = deviceDisplay
    }

    // Fetch the full name of the user by ID
    private fun fetchUserName(lastDriver: String, obdName: String, holder: DeviceViewHolder) {
        usersRef.child(lastDriver).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                if (userSnapshot.exists()) {
                    val user = userSnapshot.getValue(User::class.java)
                    val fullName = user?.getFullName() ?: lastDriver
                    holder.deviceNameTextView.text = formatDeviceNameWithDriver(obdName, fullName, true)
                } else {
                    holder.deviceNameTextView.text = "$obdName (User not found)"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                holder.deviceNameTextView.text = "$obdName (Error fetching user)"
            }
        })
    }

    // Format device name with driver's full name
    private fun formatDeviceNameWithDriver(obdName: String, driver: String, isDriverFound: Boolean): SpannableString {
        val deviceDisplay = SpannableString("$obdName (Last Driver: $driver)")
        if (isDriverFound) {
            val driverIndex = deviceDisplay.indexOf(driver)
            deviceDisplay.setSpan(StyleSpan(android.graphics.Typeface.BOLD), driverIndex, driverIndex + driver.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return deviceDisplay
    }

    // Show detailed dialog about the device
    private fun showDeviceDialog(device: ObdEntry) {
        val obdId = device.getObd_id()
        if (obdId != null) {
            obdRef.child(obdId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val obdName = dataSnapshot.child("name").value as? String ?: UNKNOWN_DEVICE
                        val status = dataSnapshot.child("status").value as? String ?: "Unknown"
                        val isAlive = dataSnapshot.child("is_alive").value as? Boolean ?: false
                        val isBusy = dataSnapshot.child("is_busy").value as? Boolean ?: false

                        val carState = when {
                            !isAlive -> "Parking"
                            isBusy -> "Driving"
                            else -> "Turned On"
                        }

                        // Create and show a dialog with clearer formatting
                        val dialogMessage = """
                            |Device Name: $obdName
                            |Last Status: $status
                            |Car State: $carState
                        """.trimMargin()

                        AlertDialog.Builder(context)
                            .setTitle("Device Information")
                            .setMessage(dialogMessage)
                            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                            .create()
                            .show()
                    } else {
                        showErrorDialog("No data found for this device.")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    showErrorDialog("Failed to fetch data.")
                }
            })
        }
    }

    // Show an error dialog
    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
