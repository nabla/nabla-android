package com.nabla.sdk.messagingsampleapp.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nabla.sdk.messagingsampleapp.databinding.ActivityMainBinding

internal class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}
