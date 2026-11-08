package com.example.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.capstoneproject.databinding.ActivitySignupBinding
import com.google.android.material.textfield.TextInputEditText

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignUp.setOnClickListener {
            handleSignUp()
        }

        binding.tvBackToLogin.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun handleSignUp() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        // Validation
        if (!validateInputs(name, email, password, confirmPassword)) {
            return
        }

        // Simulate API delay
        binding.btnSignUp.isEnabled = false
        binding.btnSignUp.text = "Creating Account..."

        Thread {
            Thread.sleep(1000)
            runOnUiThread {
                saveSignupData(name, email, password)
                showSuccessMessage()
                navigateToLogin()
            }
        }.start()
    }

    private fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        when {
            name.isEmpty() -> {
                showError("Name is required")
                return false
            }
            email.isEmpty() -> {
                showError("Email is required")
                return false
            }
            !isValidEmail(email) -> {
                showError("Please enter a valid email address")
                return false
            }
            password.isEmpty() -> {
                showError("Password is required")
                return false
            }
            password.length < 6 -> {
                showError("Password must be at least 6 characters")
                return false
            }
            confirmPassword.isEmpty() -> {
                showError("Please confirm your password")
                return false
            }
            password != confirmPassword -> {
                showError("Passwords do not match")
                return false
            }
        }
        return true
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveSignupData(name: String, email: String, password: String) {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", name)
            putString("user_email", email)
            putString("user_password", password)
            putBoolean("is_registered", true)
            apply()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccessMessage() {
        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
