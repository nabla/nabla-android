package com.nabla.sdk.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.ui.helpers.capitalize
import kotlinx.datetime.Instant

fun Provider.Companion.fake(
    id: Uuid = uuid4(),
    firstName: String = StringFaker.randomText(2, punctuation = false).capitalize(),
    lastName: String = StringFaker.randomText(2, punctuation = false).capitalize(),
    prefix: String? = "Dr",
    title: String? = "Gynécologue",
    avatar: EphemeralUrl? = EphemeralUrl(
        expiresAt = Instant.DISTANT_FUTURE,
        url = Uri("https://i.pravatar.cc/300"),
    ),
) = Provider(
    id = id,
    avatar = avatar,
    firstName = firstName,
    lastName = lastName,
    prefix = prefix,
    title = title,
)
