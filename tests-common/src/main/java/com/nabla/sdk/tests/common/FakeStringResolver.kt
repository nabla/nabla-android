package com.nabla.sdk.tests.common

import com.nabla.sdk.core.domain.boundary.StringResolver

class FakeStringResolver : StringResolver {
    override fun resolve(resId: Int): String = "fake"
}
