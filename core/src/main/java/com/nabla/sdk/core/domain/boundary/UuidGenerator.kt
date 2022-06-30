package com.nabla.sdk.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal

@NablaInternal
public interface UuidGenerator {
    public fun generate(): Uuid
}
