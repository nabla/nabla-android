package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.entity.StringId

internal class PatientRepositoryImpl(
    private val localPatientDataSource: LocalPatientDataSource,
) : PatientRepository {

    override fun setPatientId(patientId: StringId?) {
        localPatientDataSource.setPatient(patientId)
    }

    override fun getPatientId(): StringId? {
        return localPatientDataSource.getPatient()
    }
}
