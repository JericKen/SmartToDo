package com.example.smarttodo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarttodo.databinding.ActivityTaskDetailBinding
import kotlinx.coroutines.launch

class TaskDetailActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_TASK = "extra_task"
    }
    
    private lateinit var binding: ActivityTaskDetailBinding
    private var task: Task? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        
        task = intent.getParcelableExtra(EXTRA_TASK)
        if (task == null) {
            Toast.makeText(this, "Error: Task not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        populateTaskDetails()
        setupButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun populateTaskDetails() {
        task?.let { task ->
            binding.taskTitleTextView.text = task.title
            binding.taskDescriptionTextView.text = task.description
            binding.taskCategoryTextView.text = task.categoryName
            binding.taskDueDateTextView.text = task.getFormattedDueDate()
            binding.taskStatusTextView.text = if (task.isDone) "Completed" else "Pending"
            
            // Update button visibility based on task status
            if (task.isDone) {
                binding.markAsDoneButton.visibility = View.GONE
            } else {
                binding.markAsDoneButton.visibility = View.VISIBLE
            }
        }
    }
    
    private fun setupButtons() {
        binding.markAsDoneButton.setOnClickListener {
            markTaskAsDone()
        }
        
        binding.editTaskButton.setOnClickListener {
            task?.let {
                val intent = Intent(this, ManageTaskActivity::class.java).apply {
                    putExtra(ManageTaskActivity.EXTRA_TASK, it)
                }
                startActivity(intent)
            }
        }
    }
    
    private fun markTaskAsDone() {
        task?.let { currentTask ->
            binding.progressBar.visibility = View.VISIBLE
            
            lifecycleScope.launch {
                try {
                    val updatedTask = currentTask.copy(isDone = true)
                    val result = SupabaseClient.updateTask(updatedTask)
                    
                    binding.progressBar.visibility = View.GONE
                    
                    if (result.isSuccess) {
                        task = result.getOrNull()
                        Toast.makeText(this@TaskDetailActivity, "Task marked as done!", Toast.LENGTH_SHORT).show()
                        populateTaskDetails()
                    } else {
                        showError(result.exceptionOrNull()?.message ?: "Failed to update task")
                    }
                } catch (e: Exception) {
                    binding.progressBar.visibility = View.GONE
                    showError(e.message ?: "An error occurred")
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 