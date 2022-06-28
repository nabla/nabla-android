package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.StringId

internal interface PatientRepository {
    fun setPatientId(patientId: StringId?)
    fun getPatientId(): StringId?
}
