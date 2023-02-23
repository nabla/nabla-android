package com.nabla.sdk.core.data.stubs

import com.nabla.sdk.core.ui.helpers.StringExtension.capitalize
import kotlin.math.absoluteValue
import kotlin.random.Random

object StringFaker {
    fun randomText(maxWords: Int? = null, punctuation: Boolean = true) =
        "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua ut enim ad minim veniam quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat"
            .split(" ")
            .shuffled()
            .let { it.take(Random.nextInt().absoluteValue % it.size.coerceAtMost(maxWords ?: Int.MAX_VALUE) + 1) }
            .run {
                if (punctuation) {
                    mapIndexed { index, word ->
                        if (index != 0) {
                            when (Random.nextInt(6)) {
                                0 -> ", $word"
                                1 -> ". ${word.capitalize()}"
                                else -> " $word"
                            }
                        } else word.capitalize()
                    }.joinToString(separator = "")
                } else {
                    this.joinToString(separator = " ")
                }
            }
}
