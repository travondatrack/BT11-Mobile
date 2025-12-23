package com.example.baitap11

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.baitap11.data.Task
import com.example.baitap11.data.User
import com.example.baitap11.databinding.ActivityMainBinding
import com.example.baitap11.databinding.ItemTaskBinding
import com.example.baitap11.viewmodel.AppViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AppViewModel by viewModels()
    private lateinit var adapter: TaskAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        // Setup User Session (Simplified)
        val userId = intent.getIntExtra("USER_ID", -1)
        val fakeUser = User(id = userId, username = "Session", passwordHash = "")
        viewModel.currentUser = fakeUser
        if (userId >= 0) viewModel.setCurrentUserId(userId)

        adapter = TaskAdapter(
            onChecked = { task, isChecked ->
                task.isCompleted = isChecked
                viewModel.updateTask(task)
            },
            onDelete = { task ->
                viewModel.deleteTask(task)
            }
        )
        binding.rvTasks.adapter = adapter

        viewModel.tasks.observe(this) { tasks ->
            adapter.submitList(tasks)
        }

        // No direct listener needed; using data binding for new task input and add
    }
}

class TaskAdapter(
    private val onChecked: (Task, Boolean) -> Unit,
    private val onDelete: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            binding.task = task
            binding.cbCompleted.setOnCheckedChangeListener(null)
            binding.cbCompleted.isChecked = task.isCompleted
            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onChecked(task, isChecked)
            }
            binding.btnDelete.setOnClickListener { onDelete(task) }
            binding.executePendingBindings()
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
}