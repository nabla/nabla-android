package com.nabla.sdk.uitests

import android.view.View
import androidx.annotation.IdRes
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

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

fun <T> withAncestor(matcher: Matcher<T>): Matcher<T> {
    return object : BaseMatcher<T>() {
        override fun matches(item: Any): Boolean {
            return item is View && (matcher.matches(item.parent) || matches(item.parent))
        }

        override fun describeTo(description: Description) {
            description.appendText(" has an ancestor parent matching: ")
            matcher.describeTo(description)
        }
    }
}

fun withCousin(@IdRes sharedParentId: Int, matcher: Matcher<View>?) = withAncestor(
    allOf(ViewMatchers.withId(sharedParentId), ViewMatchers.withChild(matcher)),
)
