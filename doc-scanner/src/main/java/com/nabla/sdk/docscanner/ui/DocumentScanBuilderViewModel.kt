package com.nabla.sdk.docscanner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.data.helper.UrlExt.toJvmUri
import com.nabla.sdk.core.data.helper.UrlExt.toKtUri
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.kotlin.KotlinExt.runCatchingCancellable
import com.nabla.sdk.core.ui.helpers.FlowCollectorExtension.emitIn
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.mediapicker.LocalMedia
import com.nabla.sdk.docscanner.core.DocumentScanClient
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class DocumentScanBuilderViewModel(
    private val logger: Logger,
    private val documentScanClient: DocumentScanClient,
) : ViewModel() {

    val stateFlow: Flow<State>

    private val currentImageMutableFlow = MutableStateFlow<ProcessedImage?>(null)
    private val imagesMutableFlow = MutableStateFlow<List<ProcessedImage>>(emptyList())
    private val loadingMutableFlow = MutableStateFlow(false)

    private val eventMutableFlow = MutableLiveFlow<Event>()
    val eventFlow: LiveFlow<Event> = eventMutableFlow

    init {
        stateFlow = makeStateFlow()
        if (imagesMutableFlow.value.isEmpty()) {
            eventMutableFlow.emitIn(viewModelScope, Event.OpenCamera)
        }
    }

    private fun makeStateFlow(): Flow<State> {
        return combine(
            currentImageMutableFlow,
            imagesMutableFlow,
            loadingMutableFlow,
        ) { currentImage, images, loading ->
            return@combine if (loading) {
                State.Loading
            } else {
                State.Content(currentImage, images)
            }
        }.retryWhen { _, _ ->
            // No-op
            true
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = State.Loading)
    }

    fun onPictureCaptured(image: LocalMedia.Image) {
        viewModelScope.launch {
            runCatchingCancellable {
                loadingMutableFlow.emitIn(viewModelScope, true)
                val documentCorners = documentScanClient.detectDocumentCorners(image.uri.toKtUri())
                val processedImage = ProcessedImage(image, documentCorners)
                val images = imagesMutableFlow.value.toMutableList().apply { add(processedImage) }
                loadingMutableFlow.emitIn(viewModelScope, false)
                imagesMutableFlow.emitIn(viewModelScope, images)
                currentImageMutableFlow.emitIn(viewModelScope, processedImage)
                eventMutableFlow.emitIn(viewModelScope, Event.OpenEdgePicker(processedImage))
            }.onFailure { error ->
                loadingMutableFlow.emitIn(viewModelScope, false)
                logger.error("Error on detecting document", error)
                eventMutableFlow.emitIn(viewModelScope, Event.GenericError)
            }
        }
    }

    fun onErrorWithPictureCapture(error: Exception) {
        logger.error("On error with picture capture", error)
        eventMutableFlow.emitIn(viewModelScope, Event.GenericError)
    }

    fun onErrorLaunchingCameraForImageCapture(error: Throwable) {
        logger.error("On error launching image capture", error)
        eventMutableFlow.emitIn(viewModelScope, Event.GenericError)
    }

    fun onImageCaptureCancelled() {
        // No-op
    }

    fun onCropClicked() {
        currentImageMutableFlow.value?.let {
            eventMutableFlow.emitIn(viewModelScope, Event.OpenEdgePicker(it))
        }
    }

    fun onAddClicked() {
        eventMutableFlow.emitIn(viewModelScope, Event.OpenCamera)
    }

    fun onSendClicked() {
        loadingMutableFlow.emitIn(viewModelScope, true)
        viewModelScope.launch {
            runCatchingCancellable {
                val pdfUri = documentScanClient.generatePdf(imagesMutableFlow.value.map { Pair(it.image.uri.toKtUri(), it.documentCorners) })
                eventMutableFlow.emitIn(viewModelScope, Event.Finish(pdfUri))
            }.onFailure { error ->
                loadingMutableFlow.emitIn(viewModelScope, false)
                logger.error("Error on generating pdf", error)
                eventMutableFlow.emitIn(viewModelScope, Event.GenericError)
            }
        }
    }

    fun onLocalImageUpdated(localImageUri: Uri, normalizedCorners: NormalizedCorners?) {
        val imageIndex = imagesMutableFlow.value.indexOfFirst { it.image.uri == localImageUri.toJvmUri() }
        val updatedImages = imagesMutableFlow.value.toMutableList().apply {
            val updatedImage = get(imageIndex).copy(documentCorners = normalizedCorners)
            set(imageIndex, updatedImage)
        }
        imagesMutableFlow.emitIn(viewModelScope, updatedImages)
    }

    fun onScrolledTo(position: Int) {
        currentImageMutableFlow.emitIn(viewModelScope, imagesMutableFlow.value[position])
    }

    fun onDeleteClicked() {
        val currentImage = currentImageMutableFlow.value ?: return
        val images = imagesMutableFlow.value
        val indexOfImage = images.indexOf(currentImage)
        if (indexOfImage == -1) return
        imagesMutableFlow.emitIn(viewModelScope, imagesMutableFlow.value.toMutableList().apply { removeAt(indexOfImage) })
        val newCurrentImage = images.getOrNull(indexOfImage + 1) ?: images.getOrNull(indexOfImage - 1)
        currentImageMutableFlow.emitIn(viewModelScope, newCurrentImage)
    }

    sealed class State {
        data class Content(
            val currentImage: ProcessedImage?,
            val images: List<ProcessedImage>,
        ) : State()

        object Loading : State()
    }

    data class ProcessedImage(val image: LocalMedia.Image, val documentCorners: NormalizedCorners?)

    sealed class Event {
        object OpenCamera : Event()
        data class Finish(val resultUri: Uri) : Event()
        data class OpenEdgePicker(val processedImage: ProcessedImage) : Event()
        object GenericError : Event()
    }
}
