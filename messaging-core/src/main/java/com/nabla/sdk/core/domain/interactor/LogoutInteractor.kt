package com.nabla.sdk.core.domain.interactor

import com.nabla.sdk.core.domain.boundary.SessionLocalDataCleaner

internal class LogoutInteractor(
    private val sessionLocalDataCleaner: SessionLocalDataCleaner,
) {
    fun logout() {
        sessionLocalDataCleaner.cleanLocalSessionData()
    }
}
