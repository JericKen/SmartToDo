package com.example.smarttodo

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.smarttodo.databinding.ActivityManageTaskBinding
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class ManageTaskActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_TASK = "extra_task"
        
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    }
    
    private lateinit var binding: ActivityManageTaskBinding
    private var task: Task? = null
    private var categories: List<Category> = emptyList()
    private var selectedDueDate: LocalDate? = null
    private var isEditMode = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Check if editing an existing task
        task = intent.getParcelableExtra(EXTRA_TASK)
        isEditMode = task != null
        
        setupToolbar()
        loadCategories()
        setupUI()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        supportActionBar?.title = if (isEditMode) "Edit Task" else "Add Task"
    }
    
    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val userId = SupabaseClient.getCurrentUserId()
                val result = SupabaseClient.getCategories(userId)
                
                if (result.isSuccess) {
                    categories = result.getOrNull() ?: emptyList()
                    if (isEditMode) {
                        populateTaskDetails()
                    }
                } else {
                    showError(result.exceptionOrNull()?.message ?: "Failed to load categories")
                }
            } catch (e: Exception) {
                showError(e.message ?: "An error occurred")
            }
        }
    }
    
    private fun setupUI() {
        binding.dueDateButton.setOnClickListener {
            showDatePicker()
        }
        
        binding.saveTaskButton.setOnClickListener {
            saveTask()
        }
    }
    
    private fun populateTaskDetails() {
        task?.let { task ->
            binding.taskTitleEditText.setText(task.title)
            binding.taskDescriptionEditText.setText(task.description)
            
            task.dueDate?.let {
                selectedDueDate = it
                binding.dueDateButton.text = it.format(DATE_FORMATTER)
            }
            
            // Select the appropriate category radio button
            val categoryId = task.categoryId
            val categoryName = task.categoryName
            
            when (categoryName) {
                Task.CATEGORY_WORK -> binding.workRadioButton.isChecked = true
                Task.CATEGORY_SCHOOL -> binding.schoolRadioButton.isChecked = true
                Task.CATEGORY_PERSONAL -> binding.personalRadioButton.isChecked = true
            }
        }
    }
    
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        
        // Use the selected date if available, otherwise use current date
        selectedDueDate?.let {
            calendar.set(Calendar.YEAR, it.year)
            calendar.set(Calendar.MONTH, it.monthValue - 1)
            calendar.set(Calendar.DAY_OF_MONTH, it.dayOfMonth)
        }
        
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDueDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                binding.dueDateButton.text = selectedDueDate?.format(DATE_FORMATTER) ?: "Select Date"
            },
            year,
            month,
            day
        )
        
        datePickerDialog.show()
    }
    
    private fun saveTask() {
        val title = binding.taskTitleEditText.text.toString().trim()
        val description = binding.taskDescriptionEditText.text.toString().trim()
        
        if (title.isEmpty()) {
            binding.taskTitleEditText.error = "Title is required"
            return
        }
        
        val selectedRadioButtonId = binding.categoryRadioGroup.checkedRadioButtonId
        if (selectedRadioButtonId == -1) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedRadioButton = findViewById<RadioButton>(selectedRadioButtonId)
        val categoryName = selectedRadioButton.text.toString()
        val category = categories.find { it.name == categoryName }
            ?: categories.firstOrNull()
            ?: run {
                Toast.makeText(this, "No categories available", Toast.LENGTH_SHORT).show()
                return
            }
        
        val userId = SupabaseClient.getCurrentUserId()
        
        val updatedTask = if (isEditMode) {
            task!!.copy(
                title = title,
                description = description,
                dueDate = selectedDueDate,
                categoryId = category.id,
                categoryName = category.name
            )
        } else {
            Task(
                title = title,
                description = description,
                dueDate = selectedDueDate,
                isDone = false,
                categoryId = category.id,
                categoryName = category.name,
                userId = userId
            )
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val result = if (isEditMode) {
                    SupabaseClient.updateTask(updatedTask)
                } else {
                    SupabaseClient.createTask(updatedTask)
                }
                
                binding.progressBar.visibility = View.GONE
                
                if (result.isSuccess) {
                    val actionText = if (isEditMode) "updated" else "created"
                    Toast.makeText(this@ManageTaskActivity, "Task $actionText successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    showError(result.exceptionOrNull()?.message ?: "Failed to save task")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                showError(e.message ?: "An error occurred")
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
} 