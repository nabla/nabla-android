package com.nabla.sdk.messaging.ui.scene.messages

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationMediaSourcePickerBinding
import com.nabla.sdk.messaging.ui.fullscreenmedia.helper.withNablaMessagingThemeOverlays

internal class MediaSourcePickerBottomSheetFragment : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.Nabla_MediaPickerBottomSheetDialogFragmentTheme

    private var binding: NablaConversationMediaSourcePickerBinding? = null

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater =
        super.onGetLayoutInflater(savedInstanceState)
            .cloneInContext(context?.withNablaMessagingThemeOverlays())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return NablaConversationMediaSourcePickerBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState)
            .also {
                (it as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        binding.chatMediaSourcePickerOptionCameraPicture.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.CAMERA_PICTURE))
            dismiss()
        }

        binding.chatMediaSourcePickerOptionCameraVideo.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.CAMERA_VIDEO))
            dismiss()
        }

        binding.chatMediaSourcePickerOptionLibrary.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.GALLERY))
            dismiss()
        }

        binding.chatMediaSourcePickerOptionFile.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.DOCUMENT))
            dismiss()
        }

        binding.chatMediaSourcePickerOptionDocScan.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.DOCUMENT_SCAN))
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "MediaSourcePickerBottomSheetFragment:RequestKey"
        private const val RESULT_KEY = "MediaSourcePickerBottomSheetFragment:ResultKey"

        fun getResult(result: Bundle): MediaSource {
            return result.getParcelable(RESULT_KEY)!!
        }

        private fun generateResultBundle(selectedSource: MediaSource): Bundle {
            return bundleOf(RESULT_KEY to selectedSource)
        }
    }
}
