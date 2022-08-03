package com.nabla.sdk.core.annotation

@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This API is for internal use by Nabla components only"
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
public annotation class NablaInternal
