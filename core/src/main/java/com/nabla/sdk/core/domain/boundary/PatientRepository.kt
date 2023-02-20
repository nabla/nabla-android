package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.StringId
import kotlinx.coroutines.flow.Flow

internal interface PatientRepository {
    fun setPatientId(patientId: StringId?)
    fun getPatientId(): StringId?

    fun getPatientIdFlow(): Flow<StringId?>
}
