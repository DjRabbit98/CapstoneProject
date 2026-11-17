package com.example.capstoneproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.capstoneproject.R
import com.example.capstoneproject.databinding.FragmentTrainBinding
import com.example.capstoneproject.network.ApiClient
import com.example.capstoneproject.network.RecommendRequest
import com.example.capstoneproject.network.WorkoutPlan
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException

/** Training tab: asks POST /ai/recommend (kind=workout_plan) and renders the days/exercises. */
class TrainFragment : Fragment() {

    private var _binding: FragmentTrainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBuildRoutine.setOnClickListener { buildRoutine() }
    }

    private fun selectedDays(): Int = when (binding.cgDays.checkedChipId) {
        R.id.chip3 -> 3
        R.id.chip5 -> 5
        else -> 1
    }

    private fun buildRoutine() {
        val days = selectedDays()
        val extra = binding.etPlanRequest.text?.toString()?.trim()?.ifEmpty { null }
        binding.btnBuildRoutine.isEnabled = false
        binding.llPlan.removeAllViews()
        binding.llPlan.addView(caption("Building a $days-day plan (10–30 s)…"))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.recommend(RecommendRequest(kind = "workout_plan", request = extra, days = days))
                binding.llPlan.removeAllViews()
                if (!resp.isSuccessful || resp.body() == null) {
                    binding.llPlan.addView(caption("Plan failed: ${ApiClient.errorMessage(resp)}. Try again."))
                    return@launch
                }
                render(Gson().fromJson(resp.body(), WorkoutPlan::class.java))
            } catch (e: IOException) {
                binding.llPlan.removeAllViews()
                binding.llPlan.addView(caption("Can't reach the server."))
            } catch (e: Exception) {
                binding.llPlan.removeAllViews()
                binding.llPlan.addView(caption("Couldn't read the plan: ${e.message}"))
            } finally {
                if (isAdded) binding.btnBuildRoutine.isEnabled = true
            }
        }
    }

    private fun render(plan: WorkoutPlan) {
        val days = plan.days.orEmpty()
        if (days.isEmpty()) {
            binding.llPlan.addView(caption("The AI returned an empty plan. Try again."))
            return
        }
        for (day in days) {
            val card = CardView(requireContext()).apply {
                radius = 12.dp().toFloat()
                cardElevation = 2.dp().toFloat()
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 12.dp()
                layoutParams = lp
            }
            val col = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16.dp(), 16.dp(), 16.dp(), 16.dp())
            }
            val title = "Day ${day.day ?: ""}" + (day.focus?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
            col.addView(heading(title))
            day.duration_min?.let { col.addView(caption("≈ $it min")) }

            for (ex in day.exercises.orEmpty()) {
                val line = buildString {
                    append("• ").append(ex.name ?: "Exercise")
                    val bits = mutableListOf<String>()
                    ex.sets?.takeIf { it > 0 }?.let { s -> bits += "$s × ${ex.reps ?: "reps"}" }
                    ex.duration_min?.takeIf { it > 0 }?.let { bits += "$it min" }
                    ex.type?.takeIf { it.isNotBlank() }?.let { bits += it }
                    if (bits.isNotEmpty()) append("  (").append(bits.joinToString(", ")).append(")")
                }
                col.addView(body(line))
                ex.instructions?.takeIf { it.isNotBlank() }?.let { col.addView(caption(it, indent = 16)) }
            }
            card.addView(col)
            binding.llPlan.addView(card)
        }
        plan.notes?.takeIf { it.isNotBlank() }?.let { binding.llPlan.addView(caption(it)) }
        plan.disclaimer?.takeIf { it.isNotBlank() }?.let { binding.llPlan.addView(caption(it)) }
    }

    private fun heading(text: String) = TextView(requireContext()).apply {
        setText(text); setTextAppearance(R.style.TextHeading); setPadding(0, 0, 0, 4.dp())
    }

    private fun body(text: String) = TextView(requireContext()).apply {
        setText(text); setTextAppearance(R.style.TextBody); setPadding(0, 6.dp(), 0, 0)
    }

    private fun caption(text: String, indent: Int = 0) = TextView(requireContext()).apply {
        setText(text); setTextAppearance(R.style.TextCaption); setPadding(indent.dp(), 2.dp(), 0, 4.dp())
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
