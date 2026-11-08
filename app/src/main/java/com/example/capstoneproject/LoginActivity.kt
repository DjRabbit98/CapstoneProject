package com.example.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.capstoneproject.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check session
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        if (sharedPref.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Set up forgot password link
        binding.forgotPasswordLink.setOnClickListener {
            onForgotPasswordClick()
        }

        // Set up login button
        binding.loginButton.setOnClickListener {
            validateAndLogin()
        }

        // Set up create account button
        binding.createAccountButton.setOnClickListener {
            onCreateAccountClick()
        }

        // Clear error states on input
        binding.emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.emailInputLayout.error = null
            }
        }

        binding.passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.passwordInputLayout.error = null
            }
        }

        // Signup Redirect
        binding.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun validateAndLogin() {
        var isValid = true

        // Validate email
        val email = binding.emailInput.text.toString().trim()
        if (email.isBlank()) {
            binding.emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        // Validate password
        val password = binding.passwordInput.text.toString()
        if (password.isBlank()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.passwordInputLayout.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (isValid) {
            val rememberMe = binding.rememberMeCheckbox.isChecked
            performLogin(email, password, rememberMe)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun performLogin(email: String, password: String, rememberMe: Boolean) {
        // Save login state
        val sharedPref = getSharedPreferences("AppPreferences", MODE_PRIVATE)
        sharedPref.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("email", email)
            apply()
        }

        Toast.makeText(
            this,
            "Login successful for: $email",
            Toast.LENGTH_SHORT
        ).show()

        // Navigate to home screen
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun onForgotPasswordClick() {
        Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
    }

    private fun onCreateAccountClick() {
        startActivity(Intent(this, SignupActivity::class.java))
    }
}

