package com.example.smarttodo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarttodo.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private var isLoginMode = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if user is already logged in
        if (SupabaseClient.isUserLoggedIn()) {
            navigateToMainActivity()
            return
        }
        
        setupUI()
    }
    
    private fun setupUI() {
        binding.loginButton.setOnClickListener {
            if (validateInput()) {
                performAuthAction()
            }
        }
        
        binding.toggleLoginSignupText.setOnClickListener {
            toggleAuthMode()
        }
    }
    
    private fun toggleAuthMode() {
        isLoginMode = !isLoginMode
        
        if (isLoginMode) {
            binding.loginButton.text = "Login"
            binding.toggleLoginSignupText.text = "Don't have an account? Sign up"
            binding.usernameLayout.visibility = View.GONE
        } else {
            binding.loginButton.text = "Sign Up"
            binding.toggleLoginSignupText.text = "Already have an account? Login"
            binding.usernameLayout.visibility = View.VISIBLE
        }
    }
    
    private fun validateInput(): Boolean {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        
        if (email.isEmpty()) {
            binding.emailEditText.error = "Email is required"
            return false
        }
        
        if (password.isEmpty()) {
            binding.passwordEditText.error = "Password is required"
            return false
        }
        
        if (!isLoginMode) {
            val username = binding.usernameEditText.text.toString().trim()
            if (username.isEmpty()) {
                binding.usernameEditText.error = "Username is required"
                return false
            }
        }
        
        return true
    }
    
    private fun performAuthAction() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = if (isLoginMode) {
                    SupabaseClient.login(email, password)
                } else {
                    val username = binding.usernameEditText.text.toString().trim()
                    SupabaseClient.signUp(email, password, username)
                }
                
                binding.progressBar.visibility = View.GONE
                
                if (result.isSuccess) {
                    // Create default categories if signing up
                    if (!isLoginMode) {
                        createDefaultCategories(SupabaseClient.getCurrentUserId())
                    }
                    
                    navigateToMainActivity()
                } else {
                    showError(result.exceptionOrNull()?.message ?: "Authentication failed")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError(e.message ?: "An error occurred")
            }
        }
    }
    
    private suspend fun createDefaultCategories(userId: Long) {
        val categories = listOf(
            Category(name = Task.CATEGORY_WORK, userId = userId),
            Category(name = Task.CATEGORY_SCHOOL, userId = userId),
            Category(name = Task.CATEGORY_PERSONAL, userId = userId)
        )
        
        for (category in categories) {
            SupabaseClient.createCategory(category)
        }
    }
    
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 