package com.example.capstoneproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.capstoneproject.R

class ProfileFragment : Fragment() {

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchMetrics: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchNotifications = view.findViewById(R.id.swNotifications)
        switchMetrics = view.findViewById(R.id.swMetrics)
        switchDarkMode = view.findViewById(R.id.swDarkMode)

        // TODO: Implement preference logic
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Save notification preference
        }

        switchMetrics.setOnCheckedChangeListener { _, isChecked ->
            // Save metrics sharing preference
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            // Apply dark mode theme
        }

        // TODO: Load user profile data from database/API
        // TODO: Implement logout button listener
    }
}
