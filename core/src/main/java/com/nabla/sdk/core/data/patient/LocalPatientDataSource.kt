package com.nabla.sdk.core.data.patient

import android.content.SharedPreferences
import androidx.core.content.edit
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.domain.entity.toId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class LocalPatientDataSource(private val sharedPreferences: SharedPreferences) {
    private val patientIdMutableFlow = MutableStateFlow(getPatient())
    val patientIdFlow: StateFlow<StringId?> = patientIdMutableFlow

    fun setPatient(patientId: StringId?) {
        patientIdMutableFlow.value = patientId
        sharedPreferences.edit {
            putString(KEY_PATIENT_ID, patientId?.toString())
        }
    }

    fun getPatient(): StringId? {
        return sharedPreferences.getString(KEY_PATIENT_ID, null)?.toId()
    }

    companion object {
        private const val KEY_PATIENT_ID = "patient-id"
    }
}
