package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.data.local.SecuredKVStorage
import com.nabla.sdk.core.domain.entity.PatientId

internal class LocalPatientDataSource(private val securedKVStorage: SecuredKVStorage) {

    fun setPatient(patientId: PatientId?) {
        with(securedKVStorage.edit()) {
            putString(KEY_PATIENT_ID, patientId)
            apply()
        }
    }

    fun getPatient(): PatientId? {
        return securedKVStorage.getString(KEY_PATIENT_ID, null)
    }

    companion object {
        private const val KEY_PATIENT_ID = "patient-id"
    }
}