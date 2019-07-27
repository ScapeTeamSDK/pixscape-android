package com.scape.pixscape.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.scape.pixscape.R
import com.scape.pixscape.adapter.TraceHistoryAdapter
import com.scape.pixscape.models.dto.GpsTrace
import com.scape.pixscape.models.dto.ScapeTrace
import com.scape.pixscape.viewmodels.TraceViewModel
import com.scape.pixscape.viewmodels.TraceViewModelFactory
import kotlinx.android.synthetic.main.fragment_trace_history.*
import java.util.*

internal class TraceHistoryFragment : Fragment() {

    private var gpsTraces: List<GpsTrace> = Collections.emptyList()
    private var scapeTraces: List<ScapeTrace> = Collections.emptyList()

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

        viewModel.tracesList.observe(this, Observer { pairOfTraces ->
            pairOfTraces.first.observe(this, Observer { gpsValues ->
                if(gpsValues.isNotEmpty()) {
                    emptyLayout.visibility = View.GONE
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
                emptyLayout.visibility = View.VISIBLE
            } else {
                emptyLayout.visibility = View.GONE
            }
        })
        trace_history_list.adapter = adapter
        trace_history_list.layoutManager = LinearLayoutManager(activity!!.applicationContext)
    }

}
