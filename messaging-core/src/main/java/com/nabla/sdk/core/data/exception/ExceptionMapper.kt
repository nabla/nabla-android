package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.domain.entity.NablaException

internal class NablaExceptionMapper {
    private val mappers = mutableListOf<ExceptionMapper>()

    fun registerMapper(mapper: ExceptionMapper) {
        synchronized(mappers) {
            mappers.add(mapper)
        }
    }

    fun map(exception: Throwable): NablaException {
        synchronized(mappers) {
            for (mapper in mappers) {
                val mappedException = mapper.map(exception)
                if (mappedException != null) {
                    return mappedException
                }
            }
        }

        return NablaException.Unknown(exception)
    }
}

internal interface ExceptionMapper {
    fun map(exception: Throwable): NablaException?
}
