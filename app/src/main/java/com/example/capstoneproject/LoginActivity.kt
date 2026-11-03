package com.example.capstoneproject

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.capstoneproject.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var forgotPasswordLink: TextView
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button

    private var passwordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        emailInputLayout = findViewById(R.id.email_input_layout)
        emailInput = findViewById(R.id.email_input)
        passwordInputLayout = findViewById(R.id.password_input_layout)
        passwordInput = findViewById(R.id.password_input)
        rememberMeCheckbox = findViewById(R.id.remember_me_checkbox)
        forgotPasswordLink = findViewById(R.id.forgot_password_link)
        loginButton = findViewById(R.id.login_button)
        createAccountButton = findViewById(R.id.create_account_button)


        // Set up forgot password link
        forgotPasswordLink.setOnClickListener {
            onForgotPasswordClick()
        }

        // Set up login button
        loginButton.setOnClickListener {
            validateAndLogin()
        }

        // Set up create account button
        createAccountButton.setOnClickListener {
            onCreateAccountClick()
        }

        // Clear error states on input
        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                emailInputLayout.error = null
            }
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                passwordInputLayout.error = null
            }
        }
    }


    private fun validateAndLogin() {
        var isValid = true

        // Validate email
        val email = emailInput.text.toString().trim()
        if (email.isBlank()) {
            emailInputLayout.error = "Email is required"
            isValid = false
        } else if (!isValidEmail(email)) {
            emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        // Validate password
        val password = passwordInput.text.toString()
        if (password.isBlank()) {
            passwordInputLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            passwordInputLayout.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (isValid) {
            val rememberMe = rememberMeCheckbox.isChecked
            performLogin(email, password, rememberMe)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun performLogin(email: String, password: String, rememberMe: Boolean) {
        // TODO: Implement actual login logic
        // This should make an API call to authenticate the user

        Toast.makeText(
            this,
            "Login attempt for: $email\nRemember me: $rememberMe",
            Toast.LENGTH_SHORT
        ).show()

        // Example: Navigate to home screen on successful login
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun onForgotPasswordClick() {
        // TODO: Navigate to forgot password screen
        Toast.makeText(this, "Forgot password clicked", Toast.LENGTH_SHORT).show()

        // Example navigation:
        // val intent = Intent(this, ForgotPasswordScreen::class.java)
        // startActivity(intent)
    }

    private fun onCreateAccountClick() {
        // TODO: Navigate to create account screen
        Toast.makeText(this, "Create account clicked", Toast.LENGTH_SHORT).show()

        // Example navigation:
        // val intent = Intent(this, CreateAccountScreen::class.java)
        // startActivity(intent)
    }
}
