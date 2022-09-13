package com.nabla.sdk.demo.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nabla.sdk.demo.databinding.ActivityMainBinding

internal class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
