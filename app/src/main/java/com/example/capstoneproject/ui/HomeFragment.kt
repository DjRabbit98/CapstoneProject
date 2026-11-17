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
import com.example.capstoneproject.network.ApiClient
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var btnQuickWorkout: MaterialButton? = null
    private var btnLogMeal: MaterialButton? = null
    private var tvGreeting: TextView? = null
    private var tvTodayStats: TextView? = null
    private var btnOpenCoach: MaterialButton? = null

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
        btnOpenCoach = view.findViewById(R.id.btnOpenCoach)
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

        // AI Coach card
        btnOpenCoach?.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_homeFragment_to_coachFragment)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData() {
        // Same prefs file LoginActivity/SignupActivity write to.
        val sharedPref = requireContext().getSharedPreferences("AppPreferences", 0)
        val userName = sharedPref.getString("user_name", null)
            ?: sharedPref.getString("email", "User")?.substringBefore("@") ?: "User"

        tvGreeting?.text = "Good ${partOfDay()}, ${userName.replaceFirstChar { it.uppercase() }}!"
        tvTodayStats?.text = "Today: loading…"

        // Real numbers from GET /log/meals (today's view).
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.dayLog(null)
                val day = resp.body()
                if (resp.isSuccessful && day != null) {
                    val eaten = day.totals.kcal
                    val target = day.calorie_target
                    tvTodayStats?.text = if (target != null)
                        "Today: ${day.meals.size} meals • $eaten / $target kcal"
                    else
                        "Today: ${day.meals.size} meals • $eaten kcal (set your profile for a target)"
                } else {
                    tvTodayStats?.text = "Today: no data yet"
                }
            } catch (e: Exception) {
                tvTodayStats?.text = "Today: offline"
            }
        }
    }

    private fun partOfDay(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h < 12 -> "morning"
            h < 17 -> "afternoon"
            else -> "evening"
        }
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
