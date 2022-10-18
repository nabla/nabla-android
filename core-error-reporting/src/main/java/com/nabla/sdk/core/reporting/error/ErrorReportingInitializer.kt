package com.nabla.sdk.core.reporting.error

import android.content.Context
import androidx.startup.Initializer
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.ErrorReporter

@NablaInternal
public class ErrorReportingInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        ErrorReporter.reporterFactory = SentryErrorReporter.Factory()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
