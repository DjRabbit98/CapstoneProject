package com.example.capstoneproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.example.capstoneproject.R

class HomeFragment : Fragment() {

    private var btnQuickWorkout: MaterialButton? = null
    private var btnLogMeal: MaterialButton? = null
    private var tvGreeting: TextView? = null
    private var tvTodayStats: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setClickListeners()
        loadUserData()
    }

    private fun initializeViews(view: View) {
        btnQuickWorkout = view.findViewById(R.id.btnQuickWorkout)
        btnLogMeal = view.findViewById(R.id.btnLogMeal)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvTodayStats = view.findViewById(R.id.tvTodayStats)
    }

    private fun setClickListeners() {
        // Quick Workout Button
        btnQuickWorkout?.setOnClickListener {
            navigateToTraining()
        }

        // Log Meal Button
        btnLogMeal?.setOnClickListener {
            navigateToLogMeal()
        }
    }

    private fun loadUserData() {
        val sharedPref = requireContext().getSharedPreferences("app_prefs", 0)
        val userEmail = sharedPref.getString("user_email", "User") ?: "User"
        val userName = userEmail.substringBefore("@")

        tvGreeting?.text = "Good morning, ${userName.replaceFirstChar { it.uppercase() }}!"
        tvTodayStats?.text = "Today: 0 workouts • 2,100 calories consumed"
    }

    private fun navigateToTraining() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_trainingFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToLogMeal() {
        try {
            findNavController().navigate(R.id.action_homeFragment_to_logMealFragment)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
