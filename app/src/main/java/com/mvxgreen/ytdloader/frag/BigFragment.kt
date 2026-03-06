package com.mvxgreen.ytdloader.frag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mvxgreen.ytdloader.R

/**
 * Created by MVX on 7/6/2017.
 * 
 * GOAL: Initialize dialog fragment with proper layout
 * 
 * GIVEN:
 * 1) Clicked menu item id
 */
class BigFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView: View?
        val title = if (getArguments() != null) requireArguments().getString(
            getString(R.string.key_extra_menu_item_title),
            ""
        ) else ""

        // Check menu item title; inflate proper fragment
        if (title == "Enable Notifications") {
            rootView = inflater.inflate(R.layout.frag_justify_notifications, container, false)
        } else if (title == "InFlyer") {
            rootView = inflater.inflate(R.layout.frag_inflyer, container, false)
        } else {
            rootView = inflater.inflate(R.layout.frag_upgrade, container, false)
        }

        return rootView
    }

    companion object {
        private val TAG: String = BigFragment::class.java.getCanonicalName()
    }
}
