package com.example.smarttodo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: Long = 0,
    val username: String = "",
    val email: String = "",
    val createdAt: String = ""
) : Parcelable 