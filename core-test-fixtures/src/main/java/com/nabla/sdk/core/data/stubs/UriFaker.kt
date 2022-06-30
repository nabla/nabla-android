package com.nabla.sdk.core.data.stubs

import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.Uri

object UriFaker {
    fun remote() = Uri("https://random/${uuid4()}")
}
