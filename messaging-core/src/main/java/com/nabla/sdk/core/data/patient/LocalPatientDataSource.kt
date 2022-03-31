package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.data.local.SecuredKVStorage
import com.nabla.sdk.core.domain.entity.Id

internal class LocalPatientDataSource(private val securedKVStorage: SecuredKVStorage) {

    fun setPatient(patientId: Id?) {
        with(securedKVStorage.edit()) {
            putString(KEY_PATIENT_ID, patientId.toString())
            apply()
        }
    }

    fun getPatient(): Id? {
        return securedKVStorage.getString(KEY_PATIENT_ID, null)?.let { Id(it) }
    }

    companion object {
        private const val KEY_PATIENT_ID = "patient-id"
    }
}