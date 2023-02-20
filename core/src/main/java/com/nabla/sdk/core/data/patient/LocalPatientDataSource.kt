package com.nabla.sdk.core.data.patient

import android.content.SharedPreferences
import androidx.core.content.edit
import com.nabla.sdk.core.domain.entity.StringId
import com.nabla.sdk.core.domain.entity.toId
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal class LocalPatientDataSource(private val sharedPreferences: SharedPreferences) {

    val patientIdFlow: Flow<StringId?> = callbackFlow {
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_PATIENT_ID) {
                    trySend(getPatient())
                }
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
        trySend(getPatient())
        awaitClose { sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()

    fun setPatient(patientId: StringId?) {
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
