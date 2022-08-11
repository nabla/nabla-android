package com.nabla.sdk.uitests.scene

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.MessagingModule
import com.nabla.sdk.uitests.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inject stub feature modules
        NablaClient.initialize(
            modules = listOf(
                MessagingModule.Factory { nablaMessagingClientStub },
            ),
            Configuration(this, publicApiKey = "dummy")
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainer.id, StubbedInboxFragment())
                .commit()
        }
    }
}
