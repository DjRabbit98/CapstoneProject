package com.example.capstoneproject.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.example.capstoneproject.LoginActivity
import com.example.capstoneproject.R
import com.google.android.material.button.MaterialButton

class ProfileFragment : Fragment() {

    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchMetrics: SwitchCompat
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var btnLogout: MaterialButton
    private val TAG = "ProfileFragment"

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

        // logout listener
        try {
            btnLogout = view.findViewById(R.id.btnLogout)
            if (btnLogout == null) {
                Log.e(TAG, "btnLogout view not found!")
                return
            }
            btnLogout?.setOnClickListener {
                Log.d(TAG, "Logout button clicked")
                logout()
            }
        }
        catch (e: Exception) {
            Log.e(TAG, "Error in onViewCreated", e)
        }
    }

    private fun logout() {
        try {
            Log.d(TAG, "Logout initiated")
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        catch (e: Exception) {
            Log.e(TAG, "Error during logout", e)
        }
    }
}
