package com.nabla.sdk.uitests

import android.app.Application
import androidx.test.espresso.IdlingRegistry
import androidx.test.runner.AndroidJUnitRunner
import com.nabla.sdk.uitests.scene.nablaMessagingModuleStub

class UiTestRunner : AndroidJUnitRunner() {
    override fun callApplicationOnCreate(app: Application?) {
        super.callApplicationOnCreate(app)

        IdlingRegistry.getInstance().apply {
            register(nablaMessagingModuleStub.idlingRes)
        }
    }
}
