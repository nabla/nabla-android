package com.nabla.sdk.core.data.device

import android.content.SharedPreferences
import com.benasher44.uuid.Uuid

internal class InstallationDataSource(
    private val sharedPreferences: SharedPreferences,
) {
    fun getInstallIdOrNull(): Uuid? = sharedPreferences.getString(KEY_INSTALL_ID, null)?.toUuidOrNull()

    fun storeInstallId(installId: Uuid) {
        sharedPreferences.edit().putString(KEY_INSTALL_ID, installId.toString()).apply()
    }

    private fun String.toUuidOrNull() = try { Uuid.fromString(this) } catch (e: Exception) { null }

    companion object {
        private const val KEY_INSTALL_ID = "nabla:install_id"
    }
}
