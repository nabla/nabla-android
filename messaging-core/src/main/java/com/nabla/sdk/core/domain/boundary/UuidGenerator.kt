package com.nabla.sdk.core.domain.boundary

import com.benasher44.uuid.Uuid

public interface UuidGenerator {
    public fun generate(): Uuid
}
