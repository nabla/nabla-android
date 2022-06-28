package com.nabla.sdk.core.data.exception

import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.UnknownException

public class NablaExceptionMapper {
    private val mappers = mutableListOf<ExceptionMapper>()

    public fun registerMapper(mapper: ExceptionMapper) {
        synchronized(mappers) {
            mappers.add(mapper)
        }
    }

    internal fun map(exception: Throwable): NablaException {
        synchronized(mappers) {
            for (mapper in mappers) {
                val mappedException = mapper.map(exception)
                if (mappedException != null) {
                    return mappedException
                }
            }
        }

        return UnknownException(exception)
    }
}

public interface ExceptionMapper {
    public fun map(exception: Throwable): NablaException?
}
