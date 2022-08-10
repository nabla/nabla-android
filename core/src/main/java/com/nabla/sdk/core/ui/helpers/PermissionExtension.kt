package com.nabla.sdk.core.ui.helpers

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nabla.sdk.core.R
import com.nabla.sdk.core.annotation.NablaInternal

public interface PermissionRequestLauncher {
    public fun launch()
}

@NablaInternal
public data class PermissionRational(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val accept: Int = R.string.nabla_permission_rational_default_accept,
    @StringRes val decline: Int = R.string.nabla_permission_rational_default_decline,
)

@NablaInternal
public fun Fragment.registerForPermissionResult(
    permission: String,
    rational: PermissionRational,
    callback: (isGranted: Boolean) -> Unit,
): PermissionRequestLauncher = this.registerForPermissionsResult(arrayOf(permission), rational) { granted ->
    callback(granted[permission] == true)
}

@NablaInternal
public fun Fragment.registerForPermissionsResult(
    permissions: Array<String>,
    rational: PermissionRational,
    callback: (grants: Map<String, Boolean>) -> Unit,
): PermissionRequestLauncher {
    val context = this.context ?: throw PermissionException("Attempted to request permissions from a fragment not connected to any context: $this")
    val underlyingLauncher = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callback)
    return context.permissionRequestLauncher(permissions, context, callback, rational, underlyingLauncher, ::shouldShowRequestPermissionRationale)
}

@NablaInternal
public fun AppCompatActivity.registerForPermissionsResult(
    permissions: Array<String>,
    rational: PermissionRational,
    callback: (grants: Map<String, Boolean>) -> Unit,
): PermissionRequestLauncher {
    val underlyingLauncher = this.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), callback)
    return permissionRequestLauncher(permissions, this, callback, rational, underlyingLauncher, ::shouldShowRequestPermissionRationale)
}

private fun Context.permissionRequestLauncher(
    permissions: Array<String>,
    context: Context,
    callback: (grants: Map<String, Boolean>) -> Unit,
    rational: PermissionRational,
    underlyingLauncher: ActivityResultLauncher<Array<String>>,
    shouldShowRequestPermissionRationale: (permission: String) -> Boolean,
) = object : PermissionRequestLauncher {
    override fun launch() {
        val currentGrants = permissions.associateWith { (ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED) }

        val permissionsNotGranted = currentGrants.filter { !it.value }.map { it.key }.toTypedArray()
        if (permissionsNotGranted.isEmpty()) {
            callback(currentGrants)
            return
        }

        if (permissionsNotGranted.any { shouldShowRequestPermissionRationale(it) }) {
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

internal class PermissionException(message: String) : Exception(message)
