package com.nabla.sdk.docscanner.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.fragment.app.commit
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.data.helper.UrlExt.toAndroidUri
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.SceneHelpers.requireSdkName
import com.nabla.sdk.core.ui.helpers.SceneHelpers.setSdkName
import com.nabla.sdk.core.ui.helpers.ViewBindingExtension.viewBinding
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import com.nabla.sdk.docscanner.databinding.NablaDocumentScanActivityBinding
import com.nabla.sdk.core.R as CoreR

@NablaInternal
public class DocumentScanActivity : AppCompatActivity() {

    private val binding by viewBinding(NablaDocumentScanActivityBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            pushFragment(DocumentScanBuilderFragment.newInstance(intent.requireSdkName()), isFirstScreen = true)
        }
    }

    internal fun goToEdgePicker(
        localImageUri: Uri,
        normalizedCorners: NormalizedCorners?,
    ) {
        pushFragment(
            DocumentEdgePickerFragment.newInstance(
                localImageUri,
                normalizedCorners,
                intent.requireSdkName(),
            )
        )
    }

    internal fun finishWithResult(uri: Uri) {
        setResult(Activity.RESULT_OK, Intent().apply { putExtra(EXTRA_RESULT_IMAGE_URI, uri.toAndroidUri()) })
        finish()
    }

    private fun pushFragment(fragment: Fragment, isFirstScreen: Boolean = false) {
        supportFragmentManager.commit {
            if (isFirstScreen) {
                setTransition(TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN)
            } else {
                setTransition(TRANSIT_FRAGMENT_OPEN)
                setCustomAnimations(
                    CoreR.anim.nabla_slide_in_right,
                    CoreR.anim.nabla_fade_out,
                    CoreR.anim.nabla_fade_in,
                    CoreR.anim.nabla_slide_out_right,
                )
            }
            replace(binding.fragmentContainer.id, fragment)
            if (!isFirstScreen) addToBackStack(null)
        }
    }

    public companion object {
        internal const val EXTRA_RESULT_IMAGE_URI = "result_image_uri"
        public fun newIntent(context: Context, name: String): Intent =
            Intent(context, DocumentScanActivity::class.java)
                .apply {
                    setSdkName(name)
                }
    }
}
