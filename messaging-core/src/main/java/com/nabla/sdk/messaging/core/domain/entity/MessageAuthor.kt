package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.SystemUser
import com.nabla.sdk.core.domain.entity.Patient as CorePatient
import com.nabla.sdk.core.domain.entity.Provider as CoreProvider

public sealed interface MessageAuthor {
    @JvmInline
    public value class Provider(public val provider: CoreProvider) : MessageAuthor

    public sealed interface Patient : MessageAuthor {
        public val patient: CorePatient

        public object Current : Patient {
            override val patient: CorePatient.Current = CorePatient.Current
        }

        public data class Other(override val patient: CorePatient.Other) : Patient
    }

    @JvmInline
    public value class System(public val system: SystemUser) : MessageAuthor

    public object DeletedProvider : MessageAuthor

    public object Unknown : MessageAuthor
}
