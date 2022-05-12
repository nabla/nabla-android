package com.nabla.sdk.core.domain.interactor

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.SessionClient
import com.nabla.sdk.core.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.StringId

internal class LoginInteractor(
    private val patientRepository: PatientRepository,
    private val sessionClient: SessionClient,
    private val logoutInteractor: LogoutInteractor,
) {
    fun login(
        patientId: StringId,
        sessionTokenProvider: SessionTokenProvider,
    ) {
        val existingPatientId = patientRepository.getPatientId()
        if (existingPatientId != null && existingPatientId != patientId) {
            logoutInteractor.logout()
        }

        patientRepository.setPatientId(patientId)
        sessionClient.clearSession()
        sessionClient.initSession(sessionTokenProvider)
    }
}
