package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.entity.PatientId

internal class PatientRepositoryImpl(
    private val localPatientDataSource: LocalPatientDataSource,
): PatientRepository {

    override fun setPatientId(patientId: PatientId?) {
        localPatientDataSource.setPatient(patientId)
    }

    override fun getPatientId(): PatientId? {
        return localPatientDataSource.getPatient()
    }
}
