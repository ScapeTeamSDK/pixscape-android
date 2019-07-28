package com.scape.pixscape.adapter


import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.scape.pixscape.fragments.EmptyFragment
import com.scape.pixscape.fragments.TrackTraceFragment
import com.scape.pixscape.fragments.TraceHistoryFragment

internal class ViewPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment {
        //tabview position
        return when (position) {
            0 -> TraceHistoryFragment()
            1 -> EmptyFragment() // we will create the CameraFragment via the navgraph after ensuring all permissions have been granted
            2 -> TrackTraceFragment()
            else -> EmptyFragment()
        }
    }

    override fun getCount(): Int {
        //number of screens
        return 3
    }
}