package com.scape.pixscape.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.scape.pixscape.R
import com.scape.pixscape.viewmodels.TraceViewModel
import com.scape.pixscape.viewmodels.TraceViewModelFactory
import com.scape.pixscape.activities.TraceDetailsActivity
import com.scape.pixscape.fragments.CameraFragment
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.ScapeTrace
import kotlinx.android.synthetic.main.trace_history_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

internal class TraceHistoryAdapter(private val context: Context) : RecyclerView.Adapter<TraceHistoryAdapter.TraceHistoryViewHolder>() {

    private var mGpsTraces: List<GpsTrace> = Collections.emptyList()
    private var mScapeTraces: List<ScapeTrace> = Collections.emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TraceHistoryViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.trace_history_row, parent, false)
        return TraceHistoryViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return mGpsTraces.size
    }

    override fun onBindViewHolder(holder: TraceHistoryViewHolder, position: Int) {
        val gpsTrace = mGpsTraces[position]
        val scapeTrace = if(mScapeTraces.isEmpty()) ScapeTrace(Date(0L), 0L, Collections.emptyList()) else mScapeTraces[position]
        holder.setData(gpsTrace, scapeTrace)
    }

    fun setData(gpsTraces: List<GpsTrace>, scapeTraces: List<ScapeTrace>) {
        this.mGpsTraces = gpsTraces
        this.mScapeTraces = scapeTraces
        notifyDataSetChanged()
    }

    inner class TraceHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @SuppressLint("SetTextI18n")
        fun setData(gpsTrace: GpsTrace, scapeTrace: ScapeTrace) {
            itemView.history_trace_date.text = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(gpsTrace.date)
            itemView.history_distance.text = "%.2f KM".format(gpsTrace.routeSections.sumByDouble { it.distance.toDouble() } / 1000)
            val formattedTime = String.format("%02d:%02d",
                                              TimeUnit.MILLISECONDS.toHours(gpsTrace.timeInMillis),
                                              TimeUnit.MILLISECONDS.toMinutes(gpsTrace.timeInMillis) % TimeUnit.HOURS.toMinutes(1))
            itemView.history_time.text = formattedTime

            itemView.setOnClickListener {
                @Suppress("UNCHECKED_CAST") val intent = Intent(context, TraceDetailsActivity::class.java)
                        .putExtra(CameraFragment.TIME_DATA_KEY, gpsTrace.timeInMillis)
                        .putParcelableArrayListExtra(CameraFragment.ROUTE_GPS_SECTIONS_DATA_KEY,
                                                     gpsTrace.routeSections as ArrayList<Parcelable>)
                        .putParcelableArrayListExtra(CameraFragment.ROUTE_SCAPE_SECTIONS_DATA_KEY,
                                                     scapeTrace.routeSections as ArrayList<Parcelable>)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            }
            itemView.setOnLongClickListener {
                val viewModel = TraceViewModelFactory(context.applicationContext as Application).create(TraceViewModel::class.java)
                val builder = AlertDialog.Builder(context)
                builder.setMessage(R.string.confirm_delete)
                        .setPositiveButton(R.string.positive_delete) { _, _ -> viewModel.delete(gpsTrace) }
                        .setNegativeButton(R.string.negative_delete) { _, _ -> }
                builder.create()
                builder.show()
                return@setOnLongClickListener true
            }
        }
    }
}