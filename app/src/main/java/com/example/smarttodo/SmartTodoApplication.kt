package com.example.smarttodo

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class SmartTodoApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize ThreeTen library for date/time operations
        AndroidThreeTen.init(this)
        
        // Initialize Supabase client
        SupabaseClient.init(this)
    }
} 