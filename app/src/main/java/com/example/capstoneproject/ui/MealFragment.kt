package com.example.capstoneproject.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.capstoneproject.R
import com.example.capstoneproject.databinding.FragmentMealBinding
import com.example.capstoneproject.network.ApiClient
import com.example.capstoneproject.network.EstimateRequest
import com.example.capstoneproject.network.MealLogIn
import com.example.capstoneproject.network.MealPlan
import com.example.capstoneproject.network.PlanMeal
import com.example.capstoneproject.network.RecommendRequest
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.roundToInt

/**
 * Food log screen.
 *  - AI estimate from a text description  -> POST /ai/estimate (fills the form)
 *  - Manual entry                          -> POST /log/meals
 *  - Today's meals + totals                -> GET  /log/meals, DELETE /log/meals/{id}
 *  - AI meal plan with "Add" per meal      -> POST /ai/recommend kind=meal_plan
 */
class MealFragment : Fragment() {

    private var _binding: FragmentMealBinding? = null
    private val binding get() = _binding!!

    private val mealTypes = listOf("breakfast", "lunch", "dinner", "snack")
    private var lastEstimateSource = "manual"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.acMealType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1,
                mealTypes.map { it.replaceFirstChar(Char::uppercase) })
        )
        binding.acMealType.setText(guessMealType().replaceFirstChar(Char::uppercase), false)

        binding.btnAnalyse.setOnClickListener { estimate() }
        binding.btnSaveMeal.setOnClickListener { saveManual() }
        binding.btnGeneratePlan.setOnClickListener { generatePlan() }

        loadToday()
    }

    // ---------- today's log ----------

    private fun loadToday() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.dayLog(null)
                val day = resp.body()
                if (!resp.isSuccessful || day == null) {
                    binding.tvDayTotals.text = "Today: ${ApiClient.errorMessage(resp)}"
                    return@launch
                }
                val t = day.totals
                val target = day.calorie_target?.let { " of $it kcal target" } ?: ""
                binding.tvDayTotals.text =
                    "Today: ${t.kcal} kcal$target • ${t.protein_g.roundToInt()}g protein • " +
                        "${t.carbs_g.roundToInt()}g carbs • ${t.fat_g.roundToInt()}g fat"

                binding.llTodayMeals.removeAllViews()
                if (day.meals.isEmpty()) {
                    binding.llTodayMeals.addView(caption("Nothing logged yet today."))
                }
                for (m in day.meals) {
                    val row = inflateRow(
                        title = m.name,
                        sub = "${m.meal_type.replaceFirstChar(Char::uppercase)} • " +
                            "${m.protein_g?.roundToInt() ?: 0}g P / ${m.carbs_g?.roundToInt() ?: 0}g C / ${m.fat_g?.roundToInt() ?: 0}g F",
                        kcal = "${m.kcal} kcal",
                        action = "Delete"
                    ) { btn ->
                        btn.isEnabled = false
                        viewLifecycleOwner.lifecycleScope.launch {
                            try {
                                val r = ApiClient.api.deleteMeal(m.id)
                                if (r.isSuccessful) loadToday() else toast(ApiClient.errorMessage(r))
                            } catch (e: IOException) { toast("Can't reach the server") }
                            finally { if (isAdded) btn.isEnabled = true }
                        }
                    }
                    binding.llTodayMeals.addView(row)
                }
            } catch (e: IOException) {
                binding.tvDayTotals.text = "Today: can't reach the server"
            } catch (e: Exception) {
                binding.tvDayTotals.text = "Today: ${e.message}"
            }
        }
    }

    // ---------- manual entry / estimate ----------

    private fun saveManual() {
        val name = binding.etFoodName.text?.toString()?.trim().orEmpty()
        val kcal = binding.etCalories.text?.toString()?.trim()?.toIntOrNull()
        if (name.isEmpty() || kcal == null) {
            toast("Enter a food name and calories")
            return
        }
        val body = MealLogIn(
            meal_type = selectedMealType(),
            name = name,
            kcal = kcal,
            protein_g = binding.etProtein.text?.toString()?.trim()?.toDoubleOrNull(),
            carbs_g = binding.etCarbs.text?.toString()?.trim()?.toDoubleOrNull(),
            fat_g = binding.etFat.text?.toString()?.trim()?.toDoubleOrNull(),
            source = lastEstimateSource
        )
        postMeal(body, binding.btnSaveMeal) {
            binding.etFoodName.setText(""); binding.etCalories.setText("")
            binding.etProtein.setText(""); binding.etCarbs.setText(""); binding.etFat.setText("")
            binding.tvEstimateNote.visibility = View.GONE
            lastEstimateSource = "manual"
        }
    }

    private fun estimate() {
        val desc = binding.etDescribe.text?.toString()?.trim().orEmpty()
        if (desc.isEmpty()) { toast("Describe the meal first"); return }
        binding.btnAnalyse.isEnabled = false
        binding.tvEstimateNote.visibility = View.VISIBLE
        binding.tvEstimateNote.text = "Estimating…"

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.estimate(EstimateRequest(desc))
                val e = resp.body()
                if (resp.isSuccessful && e != null) {
                    binding.etFoodName.setText(e.name)
                    binding.etCalories.setText(e.kcal.toString())
                    binding.etProtein.setText(fmt(e.protein_g))
                    binding.etCarbs.setText(fmt(e.carbs_g))
                    binding.etFat.setText(fmt(e.fat_g))
                    lastEstimateSource = "ai_estimate"
                    binding.tvEstimateNote.text =
                        "AI estimate (${e.confidence} confidence). ${e.assumptions} " +
                            "Review the numbers below, then Save meal."
                } else {
                    binding.tvEstimateNote.text = "Estimate failed: ${ApiClient.errorMessage(resp)}"
                }
            } catch (e: IOException) {
                binding.tvEstimateNote.text = "Can't reach the server"
            } catch (e: Exception) {
                binding.tvEstimateNote.text = "Error: ${e.message}"
            } finally {
                if (isAdded) binding.btnAnalyse.isEnabled = true
            }
        }
    }

    // ---------- AI meal plan ----------

    private fun generatePlan() {
        binding.btnGeneratePlan.isEnabled = false
        binding.llMealPlan.removeAllViews()
        binding.llMealPlan.addView(caption("Asking the AI for a plan (10–20 s)…"))

        val extra = binding.etPlanRequest.text?.toString()?.trim()?.ifEmpty { null }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.recommend(RecommendRequest(kind = "meal_plan", request = extra, days = 1))
                binding.llMealPlan.removeAllViews()
                if (!resp.isSuccessful || resp.body() == null) {
                    binding.llMealPlan.addView(caption("Plan failed: ${ApiClient.errorMessage(resp)}. Try again."))
                    return@launch
                }
                val plan = Gson().fromJson(resp.body(), MealPlan::class.java)
                renderPlan(plan)
                // Bring the plan card (CardView = llMealPlan's grandparent) to the top of the screen.
                val card = binding.llMealPlan.parent.parent as View
                binding.svMeal.post { binding.svMeal.smoothScrollTo(0, card.top) }
            } catch (e: IOException) {
                binding.llMealPlan.removeAllViews()
                binding.llMealPlan.addView(caption("Can't reach the server."))
            } catch (e: Exception) {
                binding.llMealPlan.removeAllViews()
                binding.llMealPlan.addView(caption("Couldn't read the plan: ${e.message}"))
            } finally {
                if (isAdded) binding.btnGeneratePlan.isEnabled = true
            }
        }
    }

    private fun renderPlan(plan: MealPlan) {
        val days = plan.days.orEmpty()
        if (days.isEmpty()) {
            binding.llMealPlan.addView(caption("The AI returned an empty plan. Try again."))
            return
        }
        for (day in days) {
            if (days.size > 1) binding.llMealPlan.addView(heading("Day ${day.day ?: ""}"))
            day.total_kcal?.let { binding.llMealPlan.addView(caption("≈ $it kcal for the day")) }
            for (meal in day.meals.orEmpty()) {
                val row = inflateRow(
                    title = meal.name ?: "Meal",
                    sub = "${(meal.meal_type ?: "meal").replaceFirstChar(Char::uppercase)} • " +
                        "${meal.protein_g?.roundToInt() ?: 0}g P / ${meal.carbs_g?.roundToInt() ?: 0}g C / ${meal.fat_g?.roundToInt() ?: 0}g F",
                    kcal = "${meal.kcal ?: 0} kcal",
                    action = "Add"
                ) { btn -> addPlanMeal(meal, btn) }
                row.setOnClickListener {
                    val detail = buildString {
                        meal.ingredients?.takeIf { it.isNotEmpty() }?.let { append("Ingredients: ").append(it.joinToString(", ")) }
                        meal.instructions?.takeIf { it.isNotBlank() }?.let { if (isNotEmpty()) append("\n"); append(it) }
                    }
                    if (detail.isNotEmpty()) toast(detail)
                }
                binding.llMealPlan.addView(row)
            }
        }
        plan.notes?.takeIf { it.isNotBlank() }?.let { binding.llMealPlan.addView(caption(it)) }
        plan.disclaimer?.takeIf { it.isNotBlank() }?.let { binding.llMealPlan.addView(caption(it)) }
    }

    private fun addPlanMeal(meal: PlanMeal, btn: MaterialButton) {
        val type = meal.meal_type?.lowercase()?.takeIf { it in mealTypes } ?: "snack"
        val body = MealLogIn(
            meal_type = type,
            name = meal.name ?: "Planned meal",
            kcal = meal.kcal ?: 0,
            protein_g = meal.protein_g, carbs_g = meal.carbs_g, fat_g = meal.fat_g,
            source = "ai_plan"
        )
        postMeal(body, btn) { btn.text = "Added"; btn.isEnabled = false }
    }

    // ---------- helpers ----------

    private fun postMeal(body: MealLogIn, btn: MaterialButton, onOk: () -> Unit) {
        btn.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = ApiClient.api.addMeal(body)
                if (resp.isSuccessful) {
                    toast("Logged ${body.name} (${body.kcal} kcal)")
                    onOk()
                    loadToday()
                } else {
                    toast("Save failed: ${ApiClient.errorMessage(resp)}")
                    btn.isEnabled = true
                }
            } catch (e: IOException) {
                toast("Can't reach the server"); btn.isEnabled = true
            } catch (e: Exception) {
                toast("Error: ${e.message}"); btn.isEnabled = true
            }
        }
    }

    private fun inflateRow(
        title: String, sub: String, kcal: String, action: String, onAction: (MaterialButton) -> Unit
    ): View {
        val row = layoutInflater.inflate(R.layout.item_meal_row, binding.llTodayMeals, false)
        row.findViewById<TextView>(R.id.tvRowTitle).text = title
        row.findViewById<TextView>(R.id.tvRowSub).text = sub
        row.findViewById<TextView>(R.id.tvRowKcal).text = kcal
        val btn = row.findViewById<MaterialButton>(R.id.btnRowAction)
        btn.text = action
        btn.setOnClickListener { onAction(btn) }
        return row
    }

    private fun caption(text: String) = TextView(requireContext()).apply {
        setText(text)
        setTextAppearance(R.style.TextCaption)
        setPadding(0, 4.dp(), 0, 8.dp())
    }

    private fun heading(text: String) = TextView(requireContext()).apply {
        setText(text)
        setTextAppearance(R.style.TextHeading)
        setPadding(0, 8.dp(), 0, 4.dp())
    }

    private fun selectedMealType(): String {
        val typed = binding.acMealType.text?.toString()?.trim()?.lowercase().orEmpty()
        return if (typed in mealTypes) typed else guessMealType()
    }

    private fun guessMealType(): String {
        val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h < 10 -> "breakfast"
            h < 14 -> "lunch"
            h < 17 -> "snack"
            else -> "dinner"
        }
    }

    private fun fmt(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else String.format("%.1f", v)
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
    private fun toast(msg: String) { if (isAdded) Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show() }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
