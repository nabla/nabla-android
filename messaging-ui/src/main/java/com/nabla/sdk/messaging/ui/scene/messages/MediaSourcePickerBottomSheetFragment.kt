package com.nabla.sdk.messaging.ui.scene.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationTimelineItemMediaSourcePickerBinding

internal class MediaSourcePickerBottomSheetFragment : BottomSheetDialogFragment() {
    override fun getTheme(): Int = R.style.Nabla_MediaPickerBottomSheetDialogFragmentTheme

    lateinit var binding: NablaConversationTimelineItemMediaSourcePickerBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NablaConversationTimelineItemMediaSourcePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.chatMediaSourcePickerOptionCameraPicture.setOnClickListener {
            setFragmentResult(REQUEST_KEY, generateResultBundle(MediaSource.CAMERA_PICTURE))
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
