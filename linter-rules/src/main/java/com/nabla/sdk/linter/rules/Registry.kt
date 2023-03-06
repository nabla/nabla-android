package com.nabla.sdk.linter.rules

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

@Suppress("Unused")
public class Registry : IssueRegistry() {

    override val api: Int = CURRENT_API

    override val issues: List<Issue> get() = listOf(
        USE_ONLY_MATERIAL3_COLORS,
    )

    override val vendor: Vendor = Vendor(
        vendorName = "Nabla Android SDK team",
        feedbackUrl = "Nabla.com",
        contact = "contact@nabla.com",
    )
}
