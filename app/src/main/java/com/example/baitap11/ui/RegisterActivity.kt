package com.example.baitap11.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.baitap11.R
import com.example.baitap11.databinding.ActivityRegisterBinding
import com.example.baitap11.viewmodel.AppViewModel
import com.example.baitap11.viewmodel.LoginResult

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_register)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        binding.btnRegister.setOnClickListener {
            val user = binding.etRegUser.text.toString()
            val pass = binding.etRegPass.text.toString()
            if (user.isNotEmpty() && pass.isNotEmpty()) {
                viewModel.register(user, pass)
            }
        }

        viewModel.loginState.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> {
                    Toast.makeText(this, result.msg, Toast.LENGTH_LONG).show()
                    finish()
                }
                is LoginResult.Error -> {
                    Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
