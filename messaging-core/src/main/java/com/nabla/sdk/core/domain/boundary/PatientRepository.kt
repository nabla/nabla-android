package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.Id

interface PatientRepository {
    fun setPatientId(patientId: Id?)
    fun getPatientId(): Id?
}
