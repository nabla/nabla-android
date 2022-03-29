package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.entity.Id

internal class PatientRepositoryImpl(
    private val localPatientDataSource: LocalPatientDataSource,
) : PatientRepository {

    override fun setPatientId(patientId: Id?) {
        localPatientDataSource.setPatient(patientId)
    }

    override fun getPatientId(): Id? {
        return localPatientDataSource.getPatient()
    }
}
