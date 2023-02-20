package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.domain.boundary.PatientRepository
import com.nabla.sdk.core.domain.entity.StringId
import kotlinx.coroutines.flow.Flow

internal class PatientRepositoryImpl(
    private val localPatientDataSource: LocalPatientDataSource,
) : PatientRepository {

    override fun setPatientId(patientId: StringId?) {
        localPatientDataSource.setPatient(patientId)
    }

    override fun getPatientId(): StringId? {
        return localPatientDataSource.getPatient()
    }

    override fun getPatientIdFlow(): Flow<StringId?> {
        return localPatientDataSource.patientIdFlow
    }
}
