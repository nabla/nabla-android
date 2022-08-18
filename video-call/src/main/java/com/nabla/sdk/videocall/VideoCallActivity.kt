package com.nabla.sdk.videocall

import android.Manifest
import android.annotation.TargetApi
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_MULTIPLE_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.entity.InternalException
import com.nabla.sdk.core.ui.helpers.PermissionRational
import com.nabla.sdk.core.ui.helpers.factoryFor
import com.nabla.sdk.core.ui.helpers.launchCollect
import com.nabla.sdk.core.ui.helpers.registerForPermissionsResult
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.Both
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.None
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.RemoteOnly
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.RemoteVideo
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.SelfOnly
import com.nabla.sdk.videocall.VideoCallViewModel.VideoState.SelfVideo
import com.nabla.sdk.videocall.databinding.NablaActivityVideoCallBinding
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.track.VideoTrack
import com.nabla.sdk.core.R as CoreR

public class VideoCallActivity : AppCompatActivity() {
    private lateinit var binding: NablaActivityVideoCallBinding

    private val viewModel: VideoCallViewModel by viewModels {
        factoryFor {
            VideoCallViewModel(
                videoCallClient = intent.getVideoCallClient(),
                url = intent.getUrl(),
                token = intent.getToken(),
            )
        }
    }

    private var currentSelfTrack: VideoTrack? = null
    private var currentRemoteTrack: VideoTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkPermissions()

        binding = NablaActivityVideoCallBinding.inflate(
            layoutInflater.cloneInContext(withNablaVideoCallThemeOverlays())
        )
        setContentView(binding.root)

        binding.videoCallBackButton.setOnClickListener { onBackPressed() }

        binding.fullScreenVideoRenderer.setOnClickListener { viewModel.onFullscreenClicked() }
        binding.videoCallControlMic.setOnClickListener { viewModel.onMicClicked() }
        binding.videoCallControlCamera.setOnClickListener { viewModel.onCameraClicked() }
        binding.videoCallControlFlip.setOnClickListener { viewModel.onFlipCameraClicked() }
        binding.videoCallControlHangUp.setOnClickListener { viewModel.onHangUpClicked() }
        binding.videoCallControlFlip.isActivated = true

        launchCollect(viewModel.finishFlow, minState = Lifecycle.State.CREATED) { finishReason ->
            when (finishReason) {
                VideoCallViewModel.FinishReason.CallEnded -> Toast.makeText(this, R.string.nabla_video_call_ended, Toast.LENGTH_SHORT).show()
                VideoCallViewModel.FinishReason.PermissionsRefused -> Toast.makeText(this, R.string.nabla_video_call_error_permission_refused, Toast.LENGTH_SHORT).show()
                VideoCallViewModel.FinishReason.UnableToConnect -> Toast.makeText(this, R.string.nabla_video_call_error_failed_to_join, Toast.LENGTH_SHORT).show()
            }

            finish()
        }

        lifecycleScope.launchCollect(viewModel.controlsFlow) { controls ->
            binding.videoCallBackButton.isVisible = controls != null && controls.controlsVisible
            binding.videoCallControls.isVisible = controls != null && controls.controlsVisible
            binding.videoCallControlMic.isActivated = controls != null && controls.micEnabled
            binding.videoCallControlMic.contentDescription = if (controls?.micEnabled == true) {
                getString(R.string.nabla_video_call_controls_content_description_mic_on)
            } else {
                getString(R.string.nabla_video_call_controls_content_description_mic_off)
            }
            binding.videoCallControlCamera.isActivated = controls != null && controls.cameraEnabled
            binding.videoCallControlCamera.contentDescription = if (controls?.cameraEnabled == true) {
                getString(R.string.nabla_video_call_controls_content_description_camera_on)
            } else {
                getString(R.string.nabla_video_call_controls_content_description_camera_off)
            }
            binding.videoCallControlFlip.isEnabled = controls != null && controls.cameraEnabled

            binding.floatingMicIconOff.isVisible = controls != null && !controls.micEnabled
            binding.floatingMicIconOff.isActivated = false
        }

        lifecycleScope.launchCollect(viewModel.connectedRoomFlow) { room ->
            room?.let {
                room.initVideoRenderer(binding.floatingVideoRenderer)
                room.initVideoRenderer(binding.fullScreenVideoRenderer)
            }
        }

        lifecycleScope.launchCollect(viewModel.connectionInfoFlow) { connectionInfo ->
            when (connectionInfo) {
                VideoCallViewModel.ConnectionInfoState.Connecting -> {
                    binding.connectionInfoBanner.isVisible = true
                    binding.connectionInfoBanner.text = getString(R.string.nabla_video_call_banner_connecting)
                }
                VideoCallViewModel.ConnectionInfoState.ReConnecting -> {
                    binding.connectionInfoBanner.isVisible = true
                    binding.connectionInfoBanner.text = getString(R.string.nabla_video_call_banner_reconnecting)
                }
                VideoCallViewModel.ConnectionInfoState.Connected,
                null,
                -> binding.connectionInfoBanner.isVisible = false
            }
        }

        lifecycleScope.launchCollect(viewModel.videoStateFlow) { state ->
            releaseRemoteTrack()
            releaseSelfTrack()

            when (state) {
                is Both -> {
                    bindSelfTrack(
                        state,
                        renderer = binding.floatingVideoRenderer
                    )
                    bindRemoteTrack(
                        state,
                        renderer = binding.fullScreenVideoRenderer
                    )
                }
                None -> {
                    releaseSelfTrack()
                    releaseRemoteTrack()
                }
                is RemoteOnly -> {
                    releaseSelfTrack()
                    bindRemoteTrack(
                        state,
                        renderer = binding.fullScreenVideoRenderer
                    )
                }
                is SelfOnly -> {
                    releaseRemoteTrack()
                    bindSelfTrack(
                        state,
                        renderer = binding.fullScreenVideoRenderer
                    )
                }
            }
            binding.videoCallFloatingRendererContainer.isVisible = state is Both
            binding.fullScreenVideoRenderer.isVisible = state !is None
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        if (lifecycle.currentState == Lifecycle.State.CREATED && !isInPictureInPictureMode) {
            // When user dismisses Picture-in-Picture mode, activity lifecycle is set to CREATED (onStop).
            viewModel.onPictureInPictureDismissed()
        } else {
            viewModel.onPictureInPictureChanged(isInPictureInPictureMode)
        }
    }

    @TargetApi(26)
    private fun getPictureInPictureParams(): PictureInPictureParams {
        val sourceRectHint = Rect()
        binding.fullScreenVideoRenderer.getGlobalVisibleRect(sourceRectHint)
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(10, 16))
            .setSourceRectHint(sourceRectHint)
            .build()
    }

    private fun bindRemoteTrack(remoteVideo: RemoteVideo, renderer: TextureViewRenderer) {
        remoteVideo.remoteTrack.addRenderer(renderer)
        renderer.setMirror(false)
        currentRemoteTrack = remoteVideo.remoteTrack
    }

    private fun bindSelfTrack(selfVideo: SelfVideo, renderer: TextureViewRenderer) {
        selfVideo.selfTrack.addRenderer(renderer)
        renderer.setMirror(selfVideo.isSelfMirror)
        currentSelfTrack = selfVideo.selfTrack
    }

    private fun releaseRemoteTrack() {
        currentRemoteTrack?.removeRenderer(binding.floatingVideoRenderer)
        currentRemoteTrack?.removeRenderer(binding.fullScreenVideoRenderer)
        currentRemoteTrack = null
    }

    private fun releaseSelfTrack() {
        currentSelfTrack?.removeRenderer(binding.floatingVideoRenderer)
        currentSelfTrack?.removeRenderer(binding.fullScreenVideoRenderer)
        currentSelfTrack = null
    }

    private fun checkPermissions() {
        val permissionsLauncher = registerForPermissionsResult(
            permissions = listRequiredPermissions(),
            rational = PermissionRational(
                title = CoreR.string.nabla_conversation_camera_video_permission_rational_title,
                description = CoreR.string.nabla_conversation_camera_video_permission_rational_description,
            )
        ) { grants ->
            if (grants.values.all { it }) {
                viewModel.onPermissionsGranted()
            } else {
                viewModel.onPermissionsRefused()
            }
        }

        permissionsLauncher.launch()
    }

    private fun listRequiredPermissions() =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO) +
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Needed on API S+ to output to bluetooth devices.
                listOf(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                emptyList()
            }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getRoomId() != this.intent.getRoomId()) {
            finish()
            startActivity(intent)
        }
    }

    override fun onPictureInPictureRequested(): Boolean {
        PictureInPictureCompat.enterPictureInPictureMode(this, ::getPictureInPictureParams)
        return true
    }

    override fun onUserLeaveHint() {
        PictureInPictureCompat.enterPictureInPictureMode(this, ::getPictureInPictureParams)
    }

    override fun onBackPressed() {
        if (!PictureInPictureCompat.enterPictureInPictureMode(this, ::getPictureInPictureParams)) {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        releaseSelfTrack()
        releaseRemoteTrack()
        super.onDestroy()
    }

    internal companion object {
        private const val ARG_URL = "VIDEO_CALL_URL"
        private const val ARG_ROOM_ID = "VIDEO_CALL_ROOM_ID"
        private const val ARG_TOKEN = "VIDEO_CALL_TOKEN"
        private const val ARG_SDK_NAME = "SDK_NAME"

        private fun Intent.getUrl(): String {
            return extras?.getString(ARG_URL)
                ?: throw InternalException(IllegalStateException("Url is required"))
        }

        private fun Intent.getRoomId(): String {
            return extras?.getString(ARG_ROOM_ID)
                ?: throw InternalException(IllegalStateException("Room id is required"))
        }

        private fun Intent.getToken(): String {
            return extras?.getString(ARG_TOKEN)
                ?: throw InternalException(IllegalStateException("Token is required"))
        }

        private fun Intent.getVideoCallClient(): NablaVideoCallClient {
            val sdkName = extras?.getString(ARG_SDK_NAME) ?: throw InternalException(
                IllegalStateException("SDK name is required")
            )
            return NablaClient.getInstance(sdkName).videoCallClient
        }

        fun newIntent(
            context: Context,
            url: String,
            roomId: String,
            token: String,
            name: String,
        ): Intent = Intent(context, VideoCallActivity::class.java).apply {
            addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_MULTIPLE_TASK)
            putExtra(ARG_URL, url)
            putExtra(ARG_ROOM_ID, roomId)
            putExtra(ARG_TOKEN, token)
            putExtra(ARG_SDK_NAME, name)
        }
    }
}
