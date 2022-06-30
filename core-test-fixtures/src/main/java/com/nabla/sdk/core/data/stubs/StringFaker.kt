package com.nabla.sdk.core.data.stubs

import kotlin.math.absoluteValue
import kotlin.random.Random

object StringFaker {
    fun randomText(maxWords: Int? = null) =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
            .split(" ")
            .shuffled()
            .let { it.take(Random.nextInt().absoluteValue % it.size.coerceAtMost(maxWords ?: Int.MAX_VALUE) + 1) }
            .joinToString(separator = " ")
}
