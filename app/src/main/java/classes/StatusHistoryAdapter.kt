package classes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardriver.R
import java.text.SimpleDateFormat
import java.util.*

class StatusHistoryAdapter(private val statusHistoryList: List<StatusHistory>) : RecyclerView.Adapter<StatusHistoryAdapter.StatusHistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusHistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_status_history, parent, false)
        return StatusHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusHistoryViewHolder, position: Int) {
        val statusHistory = statusHistoryList[position]
        holder.bind(statusHistory)
    }

    override fun getItemCount(): Int {
        return statusHistoryList.size
    }

    class StatusHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)

        fun bind(statusHistory: StatusHistory) {
            statusTextView.text = statusHistory.status
            val formattedTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(statusHistory.timestamp))
            timestampTextView.text = formattedTime
        }
    }
}