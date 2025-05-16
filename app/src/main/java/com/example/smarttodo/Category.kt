package com.example.smarttodo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: Long = 0,
    val name: String = "",
    val userId: Long = 0
) : Parcelable 