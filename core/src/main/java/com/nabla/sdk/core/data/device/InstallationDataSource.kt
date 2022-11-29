package com.nabla.sdk.core.data.device

import android.content.SharedPreferences
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.StringId

internal class InstallationDataSource(
    private val unscopedSharedPreferences: SharedPreferences,
    private val legacyScopedStorage: SharedPreferences,
) {

    fun getInstallIdOrNull(userId: StringId): Uuid? = unscopedSharedPreferences.getString(getKey(userId), null)?.toUuidOrNull() ?: kotlin.run {
        // Here we read from the global storage but since we used to store the installId in the instance scoped storage we fallback on that
        val legacyInstallId = legacyScopedStorage.getString(KEY_INSTALL_ID, null)?.toUuidOrNull()
        if (legacyInstallId != null) {
            storeInstallId(legacyInstallId, userId)
            legacyScopedStorage.edit().remove(KEY_INSTALL_ID).apply()
        }

        return@run legacyInstallId
    }

    fun storeInstallId(installId: Uuid, userId: StringId) {
        unscopedSharedPreferences.edit().putString(getKey(userId), installId.toString()).apply()
    }

    private fun String.toUuidOrNull() = try { Uuid.fromString(this) } catch (e: Exception) { null }

    companion object {
        private const val KEY_INSTALL_ID = "nabla:install_id"

        private fun getKey(userId: StringId): String = "${KEY_INSTALL_ID}_${userId.value.hashCode()}"
    }
}
