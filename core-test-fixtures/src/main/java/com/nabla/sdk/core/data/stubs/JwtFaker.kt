package com.nabla.sdk.core.data.stubs

import com.nabla.sdk.core.domain.entity.AuthTokens

object JwtFaker {
    // Use https://www.javainuse.com/jwtgenerator to easily generate mocked tokens
    const val expiredIn2050 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.a9B-ZzVUPI04w6AjKZ9ODvU7P8s4G6SqpQnfaei5EaE"
    const val expiredIn2050_2 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.nqK7fOSd0WcVk3HYlbQuK8jindWlao4QTp8E2CWhIdg"
    const val expiredIn2050_3 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjI1MjE4NDMyMDAsImlhdCI6OTQzOTIwMDAwfQ.SBykkJNK3avicHjw16uHCxFUYmbp_YpLc34YsC31eu0"

    const val expiredIn2020 = "eyJhbGciOiJIUzI1NiJ9.eyJSb2xlIjoiUm9sZSIsIklzc3VlciI6Iklzc3VlciIsIlVzZXJuYW1lIjoiVXNlcm5hbWUiLCJleHAiOjE1NzUwNzIwMDAsImlhdCI6OTQzOTIwMDAwfQ.tITlAVJAI8LX1Fi0FStSJWf5z45Vs8mXoXlfpaTnR9c"
    const val expiredIn2020_2 = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE1Nzc4MzY4MDAsImlhdCI6MTU3NzgzNjgwMH0.xHfoyMAIvzB1sk3s04Z-BhOviUCMPz2QzM3qL3vuHcU"
}

fun AuthTokens.Companion.fake() = AuthTokens(
    refreshToken = JwtFaker.expiredIn2050,
    accessToken = JwtFaker.expiredIn2050_2,
)
