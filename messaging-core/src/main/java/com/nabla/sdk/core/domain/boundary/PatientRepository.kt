package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.PatientId

interface PatientRepository {
    fun setPatientId(patientId: PatientId?)
    fun getPatientId(): PatientId?
}
