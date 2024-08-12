import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import classes.ObdEntry
import com.example.cardriver.R

class DeviceAdapter(private val context: Context, private val devices: List<ObdEntry>) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val deviceNameTextView: TextView = itemView.findViewById(R.id.deviceNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.deviceNameTextView.text = device.getObd_id()

        // Set click listener for each item
        holder.itemView.setOnClickListener {
            showDeviceDialog(device)
        }
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    private fun showDeviceDialog(device: ObdEntry) {
        AlertDialog.Builder(context)
            .setTitle("Device Info")
            .setMessage("Device ID: ${device.getObd_id()}")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }
}
