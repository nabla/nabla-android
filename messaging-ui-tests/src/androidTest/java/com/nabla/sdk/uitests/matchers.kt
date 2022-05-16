package com.nabla.sdk.uitests

import org.hamcrest.BaseMatcher
import org.hamcrest.Matcher

fun <T> first(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        var isFirst = true

        override fun matches(item: Any): Boolean {
            if (isFirst && matcher.matches(item)) {
                isFirst = false
                return true
            }
            return false
        }

        override fun describeTo(description: org.hamcrest.Description) {
            description.appendText("should return first matching item")
        }
    }
}
