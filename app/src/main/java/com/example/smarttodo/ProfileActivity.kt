package com.example.smarttodo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarttodo.databinding.ActivityProfileBinding
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfileBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        displayUserInfo()
        loadTaskStatistics()
        setupLogoutButton()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun displayUserInfo() {
        binding.usernameTextView.text = SupabaseClient.getCurrentUsername()
        binding.emailTextView.text = SupabaseClient.getCurrentUserEmail()
    }
    
    private fun loadTaskStatistics() {
        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.getCurrentUserId()
                val tasksResult = SupabaseClient.getTasks(userId)
                
                if (tasksResult.isSuccess) {
                    val tasks = tasksResult.getOrNull() ?: emptyList()
                    
                    val totalTasks = tasks.size
                    val completedTasks = tasks.count { it.isDone }
                    val pendingTasks = totalTasks - completedTasks
                    
                    binding.totalTasksTextView.text = totalTasks.toString()
                    binding.completedTasksTextView.text = completedTasks.toString()
                    binding.pendingTasksTextView.text = pendingTasks.toString()
                } else {
                    showError(tasksResult.exceptionOrNull()?.message ?: "Failed to load task statistics")
                }
            } catch (e: Exception) {
                showError(e.message ?: "An error occurred")
            }
        }
    }
    
    private fun setupLogoutButton() {
        binding.logoutButton.setOnClickListener {
            SupabaseClient.logout()
            
            // Navigate to login
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 