package com.example.baitap11.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.baitap11.data.AppRepository
import com.example.baitap11.data.Task
import com.example.baitap11.data.User
import com.example.baitap11.utils.SecurityUtils
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AppRepository.getInstance(application)

    private val _loginState = MutableLiveData<LoginResult>()
    val loginState: LiveData<LoginResult> = _loginState

    var currentUser: User? = null

    private val _currentUserId = MutableLiveData<Int>()
    fun setCurrentUserId(id: Int) { _currentUserId.value = id }

    // Two-way bound text for new task input
    val newTaskText = MutableLiveData<String>("")

    // Expose tasks LiveData by switching on current user id
    val tasks: LiveData<List<Task>> = _currentUserId.switchMap { id ->
        repo.observeTasksByUser(id)
    }

    fun addTaskFromBinding() {
        val title = newTaskText.value ?: ""
        if (title.isNotBlank()) {
            addTask(title)
            newTaskText.value = ""
        }
    }

    fun register(username: String, pass: String) {
        viewModelScope.launch {
            if (repo.getUserByUsername(username) != null) {
                _loginState.value = LoginResult.Error("Username already exists")
                return@launch
            }
            val hashed = SecurityUtils.hashPassword(pass)
            val newUser = User(username = username, passwordHash = hashed)
            repo.insertUser(newUser)
            _loginState.value = LoginResult.Success("Registration successful! Please login.")
        }
    }

    fun login(username: String, pass: String) {
        viewModelScope.launch {
            val user = repo.getUserByUsername(username)
            if (user != null) {
                if (user.passwordHash == SecurityUtils.hashPassword(pass)) {
                    currentUser = user
                    _currentUserId.value = user.id
                    _loginState.value = LoginResult.LoggedIn(user)
                } else {
                    _loginState.value = LoginResult.Error("Wrong password")
                }
            } else {
                _loginState.value = LoginResult.Error("User not found")
            }
        }
    }

    fun addTask(title: String) {
        currentUser?.let { user ->
            viewModelScope.launch {
                repo.insertTask(Task(title = title, userId = user.id))
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repo.updateTask(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repo.deleteTask(task)
        }
    }
}

sealed class LoginResult {
    data class Success(val msg: String) : LoginResult()
    data class Error(val error: String) : LoginResult()
    data class LoggedIn(val user: User) : LoginResult()
}