package com.nabla.sdk.core.data.patient

import com.nabla.sdk.core.data.local.SecuredKVStorage
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.domain.entity.toId

internal class LocalPatientDataSource(private val securedKVStorage: SecuredKVStorage) {

    fun setPatient(patientId: StringId?) {
        with(securedKVStorage.edit()) {
            putString(KEY_PATIENT_ID, patientId.toString())
            apply()
        }
    }

    fun getPatient(): StringId? {
        return securedKVStorage.getString(KEY_PATIENT_ID, null)?.toId()
    }

    companion object {
        private const val KEY_PATIENT_ID = "patient-id"
    }
}
