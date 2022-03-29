package com.nabla.sdk.core.domain.interactor

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.boundary.TokenRepository
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.kotlin.runCatchingCancellable

class LoginInteractor(
    private val patientRepository: PatientRepository,
    private val tokenRepository: TokenRepository,
) {

    suspend operator fun invoke(patientId: Id): Result<Unit> {
        return runCatchingCancellable {
            patientRepository.setPatientId(patientId)
            tokenRepository.clearSession()
            tokenRepository.getFreshAccessToken(forceRefreshAccessToken = true)
        }
    }
}
