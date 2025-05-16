package com.example.smarttodo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

@Parcelize
data class Task(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val dueDate: LocalDate? = null,
    val isDone: Boolean = false,
    val categoryId: Long = 0,
    val categoryName: String = "",
    val userId: Long = 0,
    val createdAt: String = ""
) : Parcelable {

    fun getFormattedDueDate(): String {
        return dueDate?.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) ?: ""
    }

    companion object {
        const val CATEGORY_WORK = "Work"
        const val CATEGORY_SCHOOL = "School"
        const val CATEGORY_PERSONAL = "Personal"
    }
} 