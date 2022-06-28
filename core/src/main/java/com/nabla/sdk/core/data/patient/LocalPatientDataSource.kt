package com.nabla.sdk.core.data.patient

import android.content.SharedPreferences
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.domain.entity.toId

internal class LocalPatientDataSource(private val sharedPreferences: SharedPreferences) {

    fun setPatient(patientId: StringId?) {
        with(sharedPreferences.edit()) {
            putString(KEY_PATIENT_ID, patientId.toString())
            apply()
        }
    }

    fun getPatient(): StringId? {
        return sharedPreferences.getString(KEY_PATIENT_ID, null)?.toId()
    }

    companion object {
        private const val KEY_PATIENT_ID = "patient-id"
    }
}
