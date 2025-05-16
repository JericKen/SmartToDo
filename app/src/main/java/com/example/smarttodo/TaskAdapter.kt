package com.example.smarttodo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: List<Task> = emptyList(),
    private val onTaskClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view, onTaskClick)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskClick: (Task) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val titleTextView: TextView = itemView.findViewById(R.id.taskTitleTextView)
        private val dueDateTextView: TextView = itemView.findViewById(R.id.taskDueDateTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.taskCategoryTextView)
        private val remarksTextView: TextView = itemView.findViewById(R.id.taskRemarksTextView)
        private val statusCheckBox: CheckBox = itemView.findViewById(R.id.taskStatusCheckBox)
        
        private var currentTask: Task? = null
        
        init {
            itemView.setOnClickListener {
                currentTask?.let { onTaskClick(it) }
            }
        }
        
        fun bind(task: Task) {
            currentTask = task
            
            titleTextView.text = task.title
            dueDateTextView.text = "Due: ${task.getFormattedDueDate()}"
            categoryTextView.text = task.categoryName
            remarksTextView.text = task.description
            statusCheckBox.isChecked = task.isDone
        }
    }
} 