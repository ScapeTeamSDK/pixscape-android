package com.scape.pixscape.fragments

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.scape.pixscape.R
import com.scape.pixscape.adapter.TraceHistoryAdapter
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.ScapeTrace
import com.scape.pixscape.viewmodels.TraceViewModel
import com.scape.pixscape.viewmodels.TraceViewModelFactory
import kotlinx.android.synthetic.main.fragment_trace_history.*
import xyz.sangcomz.stickytimelineview.RecyclerSectionItemDecoration
import xyz.sangcomz.stickytimelineview.ext.DP
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.text.SimpleDateFormat
import java.util.*

internal class TraceHistoryFragment : Fragment() {

    private var gpsTraces: List<GpsTrace> = Collections.emptyList()
    private var scapeTraces: List<ScapeTrace> = Collections.emptyList()

    private fun getOvalDrawable(): Drawable {
        val radius = 8.DP(this.context!!).toInt()
        val strokeWidth =  radius / 2
        val roundRadius = radius * 2
        val strokeColor = ContextCompat.getColor(context!!, R.color.color_white)
        val fillColor = ContextCompat.getColor(context!!, R.color.scape_color)

        val gd = GradientDrawable()
        gd.setColor(fillColor)
        gd.cornerRadius = roundRadius.toFloat()
        gd.setStroke(strokeWidth, strokeColor)

        return gd
    }

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trace_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TraceHistoryAdapter(activity!!)
        val viewModel = TraceViewModelFactory(activity!!.application).create(TraceViewModel::class.java)

        val emptyLayout = this.activity!!.findViewById<View>(R.id.empty_trace_layout)

        traces_list_title_dot.background = getOvalDrawable()

        viewModel.tracesList.observe(this, Observer { pairOfTraces ->
            pairOfTraces.first.observe(this, Observer { gpsValues ->
                if(gpsValues.isNotEmpty()) {
                    emptyLayout?.visibility = View.GONE
                }
                gpsTraces = gpsValues
                adapter.setData(gpsTraces, scapeTraces)
            })

            pairOfTraces.second.observe(this, Observer { scapeValues ->
                scapeTraces = scapeValues
                adapter.setData(gpsTraces, scapeTraces)

                // we assume there will always be more GPS traces than Scape traces so we don't check
                // Scape traces
            })

            if(gpsTraces.isEmpty()) {
                emptyLayout?.visibility = View.VISIBLE
            } else {
                emptyLayout?.visibility = View.GONE
            }
        })
        trace_history_list.layoutManager = LinearLayoutManager(activity!!.applicationContext,
                                                               RecyclerView.VERTICAL,
                                                               false)
        val itemDecorationCount = trace_history_list.itemDecorationCount
        if(itemDecorationCount > 0) {
            trace_history_list.removeItemDecorationAt(itemDecorationCount - 1)
        } else {
            trace_history_list.addItemDecoration(getSectionCallback())
        }
        trace_history_list.adapter = adapter
    }

    private fun getSectionCallback(): RecyclerSectionItemDecoration.SectionCallback {
        return object : RecyclerSectionItemDecoration.SectionCallback {

            override fun isSection(position: Int): Boolean {
                val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(gpsTraces[position].date)
                val previousDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(gpsTraces[position - 1].date)

                return currentDate != previousDate
            }

            override fun getSectionHeader(position: Int): SectionInfo? =
                    SectionInfo(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(gpsTraces[position].date), "")
        }
    }

}
