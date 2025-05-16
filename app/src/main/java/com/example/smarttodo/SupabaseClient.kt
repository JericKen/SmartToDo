package com.example.smarttodo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.LocalDate
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type

object SupabaseClient {
    private const val TAG = "SupabaseClient"
    private const val SUPABASE_URL = "https://xkklbdkejvzowfhcttli.supabase.co"
    private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inhra2xiZGtlanZ6b3dmaGN0dGxpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDcyNjQxNDksImV4cCI6MjA2Mjg0MDE0OX0.i2CjiUvH0iWROMF6lZbF3yl3bsZTpDfl67UMYHlKF5M"
    private const val PREF_NAME = "SmartTodoPrefs"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_ACCESS_TOKEN = "access_token"

    private lateinit var prefs: SharedPreferences
    private lateinit var apiService: SupabaseApiService
    
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("apikey", SUPABASE_API_KEY)
                .header("Content-Type", "application/json")
                
            // Add Authorization header if token exists
            val token = prefs.getString(KEY_ACCESS_TOKEN, null)
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
            
            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        val retrofit = Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(SupabaseApiService::class.java)
    }
    
    // User Authentication
    suspend fun signUp(email: String, password: String, username: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = SignUpRequest(email, password, mapOf("username" to username))
            val response = apiService.signUp(request)
            
            if (response.error != null) {
                return@withContext Result.failure(Exception(response.error.message))
            }
            
            // After sign up, login to get session
            return@withContext login(email, password)
        } catch (e: Exception) {
            Log.e(TAG, "Error signing up: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(email, password)
            val response = apiService.login(request)
            
            if (response.error != null) {
                return@withContext Result.failure(Exception(response.error.message))
            }
            
            val session = response.session
            val token = session?.accessToken
            val userData = response.user
            
            if (token == null || userData == null) {
                return@withContext Result.failure(Exception("Failed to get user data or token"))
            }
            
            // Save token
            prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
            
            // Extract user info
            val userId = userData.id.toLongOrNull() ?: return@withContext Result.failure(Exception("Invalid user ID"))
            val userMetadata: Map<String, Any> = userData.userMetadata ?: emptyMap()
            val username = userMetadata["username"]?.toString() ?: email.substringBefore('@')
            
            val user = User(userId, username, email, System.currentTimeMillis().toString())
            saveUserToPrefs(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging in: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun logout() {
        try {
            prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
            clearUserFromPrefs()
        } catch (e: Exception) {
            Log.e(TAG, "Error logging out: ${e.message}")
        }
    }
    
    // Task Operations
    suspend fun getTasks(userId: Long): Result<List<Task>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTasks(userId)
            
            val tasks = response.map { item ->
                mapToTask(item)
            }
            
            Result.success(tasks)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun createTask(task: Task): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val taskMap = mapOf(
                "title" to task.title,
                "description" to task.description,
                "due_date" to task.dueDate?.toString(),
                "is_done" to task.isDone,
                "category_id" to task.categoryId,
                "user_id" to task.userId,
                "created_at" to System.currentTimeMillis().toString()
            )
            
            val response = apiService.createTask(taskMap)
            Result.success(mapToTask(response[0]))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updateTask(task: Task): Result<Task> = withContext(Dispatchers.IO) {
        try {
            val taskMap = mapOf(
                "title" to task.title,
                "description" to task.description,
                "due_date" to task.dueDate?.toString(),
                "is_done" to task.isDone,
                "category_id" to task.categoryId
            )
            
            val response = apiService.updateTask(task.id, taskMap)
            Result.success(mapToTask(response[0]))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun deleteTask(taskId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteTask(taskId)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Category Operations
    suspend fun getCategories(userId: Long): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCategories(userId)
            
            val categories = response.map { item ->
                mapToCategory(item)
            }
            
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting categories: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun createCategory(category: Category): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val categoryMap = mapOf(
                "name" to category.name,
                "user_id" to category.userId
            )
            
            val response = apiService.createCategory(categoryMap)
            Result.success(mapToCategory(response[0]))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating category: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Helper functions
    private fun mapToTask(map: Map<String, Any>): Task {
        val id = (map["id"] as? Number)?.toLong() ?: 0
        val title = map["title"] as? String ?: ""
        val description = map["description"] as? String ?: ""
        val dueDateStr = map["due_date"] as? String
        val dueDate = if (dueDateStr != null) LocalDate.parse(dueDateStr) else null
        val isDone = map["is_done"] as? Boolean ?: false
        val categoryId = (map["category_id"] as? Number)?.toLong() ?: 0
        val userId = (map["user_id"] as? Number)?.toLong() ?: 0
        val createdAt = map["created_at"] as? String ?: ""
        
        return Task(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            isDone = isDone,
            categoryId = categoryId,
            userId = userId,
            createdAt = createdAt
        )
    }
    
    private fun mapToCategory(map: Map<String, Any>): Category {
        val id = (map["id"] as? Number)?.toLong() ?: 0
        val name = map["name"] as? String ?: ""
        val userId = (map["user_id"] as? Number)?.toLong() ?: 0
        
        return Category(
            id = id,
            name = name,
            userId = userId
        )
    }
    
    // User preferences management
    private fun saveUserToPrefs(user: User) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_EMAIL, user.email)
            .apply()
    }
    
    private fun clearUserFromPrefs() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_EMAIL)
            .apply()
    }
    
    fun getCurrentUserId(): Long {
        return prefs.getLong(KEY_USER_ID, 0)
    }
    
    fun getCurrentUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }
    
    fun getCurrentUserEmail(): String {
        return prefs.getString(KEY_EMAIL, "") ?: ""
    }
    
    fun isUserLoggedIn(): Boolean {
        return getCurrentUserId() != 0L && prefs.getString(KEY_ACCESS_TOKEN, null) != null
    }
}

// Data classes for requests and responses
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val session: Session?,
    val user: SupabaseUser?,
    val error: ErrorResponse?
)

data class Session(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String
)

data class SupabaseUser(
    val id: String,
    val email: String,
    @SerializedName("user_metadata")
    val userMetadata: Map<String, Any>?
)

data class ErrorResponse(
    val message: String
)

// Retrofit API interface
interface SupabaseApiService {
    @POST("/auth/v1/signup")
    suspend fun signUp(@Body request: SignUpRequest): AuthResponse

    @POST("/auth/v1/token?grant_type=password")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @GET("/rest/v1/tasks")
    suspend fun getTasks(
        @Query("user_id") userId: Long,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any>>

    @POST("/rest/v1/tasks")
    suspend fun createTask(
        @Body task: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any>>

    @PATCH("/rest/v1/tasks")
    suspend fun updateTask(
        @Query("id") taskId: Long,
        @Body task: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any>>

    @DELETE("/rest/v1/tasks")
    suspend fun deleteTask(
        @Query("id") taskId: Long
    )

    @GET("/rest/v1/categories")
    suspend fun getCategories(
        @Query("user_id") userId: Long,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any>>

    @POST("/rest/v1/categories")
    suspend fun createCategory(
        @Body category: Map<String, Any?>,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any>>
} 