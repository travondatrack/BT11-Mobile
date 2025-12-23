package com.example.baitap11.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Data models used by repository and UI
data class User(
    val id: Int = 0,
    val username: String,
    val passwordHash: String
)

data class Task(
    val id: Int = 0,
    var title: String,
    var isCompleted: Boolean = false,
    val userId: Int
)

// Simple SQLiteOpenHelper-based persistence to avoid Room/kapt complexity.

private const val DB_NAME = "todolist_secure.db"
private const val DB_VERSION = 1

class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                user_id INTEGER NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            );
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // simple reset for development
        db.execSQL("DROP TABLE IF EXISTS tasks")
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }
}

class AppRepository private constructor(context: Context) {
    private val dbHelper = DbHelper(context.applicationContext)

    // LiveData cache per userId
    private val tasksLiveDataMap = mutableMapOf<Int, MutableLiveData<List<Task>>>()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AppRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun getUserByUsername(username: String): User? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        var user: User? = null
        val cursor: Cursor = db.query(
            "users",
            arrayOf("id", "username", "password_hash"),
            "username = ?",
            arrayOf(username),
            null, null, null, "1"
        )
        cursor.use {
            if (it.moveToFirst()) {
                user = User(
                    id = it.getInt(it.getColumnIndexOrThrow("id")),
                    username = it.getString(it.getColumnIndexOrThrow("username")),
                    passwordHash = it.getString(it.getColumnIndexOrThrow("password_hash"))
                )
            }
        }
        user
    }

    suspend fun insertUser(user: User): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("username", user.username)
            put("password_hash", user.passwordHash)
        }
        db.insertOrThrow("users", null, cv)
    }

    suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("is_completed", if (task.isCompleted) 1 else 0)
            put("user_id", task.userId)
        }
        db.insert("tasks", null, cv)
        refreshTasksForUser(task.userId)
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put("title", task.title)
            put("is_completed", if (task.isCompleted) 1 else 0)
        }
        db.update("tasks", cv, "id = ?", arrayOf(task.id.toString()))
        refreshTasksForUser(task.userId)
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("tasks", "id = ?", arrayOf(task.id.toString()))
        refreshTasksForUser(task.userId)
    }

    fun observeTasksByUser(userId: Int): LiveData<List<Task>> {
        return tasksLiveDataMap.getOrPut(userId) {
            val live = MutableLiveData<List<Task>>()
            // initial load
            live.value = loadTasksByUserSync(userId)
            live
        }
    }

    private fun loadTasksByUserSync(userId: Int): List<Task> {
        val db = dbHelper.readableDatabase
        val list = mutableListOf<Task>()
        val cursor = db.query(
            "tasks",
            arrayOf("id", "title", "is_completed", "user_id"),
            "user_id = ?",
            arrayOf(userId.toString()),
            null, null, "id DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Task(
                        id = it.getInt(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        isCompleted = it.getInt(it.getColumnIndexOrThrow("is_completed")) != 0,
                        userId = it.getInt(it.getColumnIndexOrThrow("user_id"))
                    )
                )
            }
        }
        return list
    }

    private fun refreshTasksForUser(userId: Int) {
        tasksLiveDataMap[userId]?.postValue(loadTasksByUserSync(userId))
    }
}

// Provide compatibility functions so ViewModel can easily use this repo like a DAO

suspend fun AppDatabase_getRepository(context: Context): AppRepository {
    return AppRepository.getInstance(context)
}
