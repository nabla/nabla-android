package com.nabla.sdk.messaging.ui.helper

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.messaging.ui.R

internal interface PermissionRequestLauncher {
    fun launch()
}

internal data class PermissionRational(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val accept: Int = R.string.nabla_conversation_message_copy_label, // R.string.permission_rational_default_accept,
    @StringRes val decline: Int = R.string.nabla_conversation_message_copy_label, // R.string.permission_rational_default_decline,
)

internal fun Fragment.registerForPermissionResult(
    permission: String,
    rational: PermissionRational,
    callback: (isGranted: Boolean) -> Unit,
): PermissionRequestLauncher = this.registerForPermissionsResult(arrayOf(permission), rational) { granted ->
    callback(granted[permission] == true)
}

internal fun Fragment.registerForPermissionsResult(
    permissions: Array<String>,
    rational: PermissionRational,
    callback: (grants: Map<String, Boolean>) -> Unit,
): PermissionRequestLauncher {
    val context = this.context ?: throw PermissionException("Attempted to request permissions from a fragment not connected to any context: $this")
    val underlyingLauncher = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callback)
    return object : PermissionRequestLauncher {
        override fun launch() {
            val currentGrants = permissions.associate { it to (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED) }

            val permissionsNotGranted = currentGrants.filter { !it.value }.map { it.key }.toTypedArray()
            if (permissionsNotGranted.isEmpty()) {
                callback(currentGrants)
                return
            }

            if (permissionsNotGranted.any { this@registerForPermissionsResult.shouldShowRequestPermissionRationale(it) }) {
                MaterialAlertDialogBuilder(context)
                    .setTitle(getString(rational.title))
                    .setMessage(getString(rational.description))
                    .setPositiveButton(rational.accept) { dialog, _ ->
                        dialog.dismiss()
                        underlyingLauncher.launch(permissionsNotGranted)
                    }
                    .setNegativeButton(rational.decline) { dialog, _ ->
                        dialog.dismiss()
                        callback(currentGrants)
                    }
                    .show()
            } else {
                underlyingLauncher.launch(permissionsNotGranted)
            }
        }
    }
}

internal class PermissionException(message: String) : Exception(message)
