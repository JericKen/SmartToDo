package com.example.smarttodo

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarttodo.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in
        if (!SupabaseClient.isUserLoggedIn()) {
            navigateToLogin()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRecyclerView()
        setupAddTaskButton()
        
        loadTasks()
    }
    
    override fun onResume() {
        super.onResume()
        loadTasks()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                loadTasks()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }
    
    private fun setupRecyclerView() {
        adapter = TaskAdapter { task ->
            navigateToTaskDetail(task)
        }
        
        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }
    
    private fun setupAddTaskButton() {
        binding.addTaskFab.setOnClickListener {
            val intent = Intent(this, ManageTaskActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun loadTasks() {
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.getCurrentUserId()
                val categoriesResult = SupabaseClient.getCategories(userId)
                val tasksResult = SupabaseClient.getTasks(userId)
                
                binding.progressBar.visibility = View.GONE
                
                if (categoriesResult.isSuccess && tasksResult.isSuccess) {
                    val categories = categoriesResult.getOrNull() ?: emptyList()
                    val tasks = tasksResult.getOrNull() ?: emptyList()
                    
                    // Combine tasks with category names
                    val tasksWithCategories = tasks.map { task ->
                        val category = categories.find { it.id == task.categoryId }
                        task.copy(categoryName = category?.name ?: "")
                    }
                    
                    updateUI(tasksWithCategories)
                } else {
                    showError("Failed to load tasks")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError(e.message ?: "An error occurred")
            }
        }
    }
    
    private fun updateUI(tasks: List<Task>) {
        adapter.updateTasks(tasks)
        
        if (tasks.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.tasksRecyclerView.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.tasksRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun navigateToTaskDetail(task: Task) {
        val intent = Intent(this, TaskDetailActivity::class.java).apply {
            putExtra(TaskDetailActivity.EXTRA_TASK, task)
        }
        startActivity(intent)
    }
    
    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}