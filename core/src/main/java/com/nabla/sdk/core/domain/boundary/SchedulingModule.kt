package com.nabla.sdk.core.domain.boundary

import android.content.Context
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.ModuleType

public interface SchedulingModule : Module<SchedulingInternalClient> {

    public fun interface Factory : Module.Factory<SchedulingModule> {
        override fun type(): ModuleType = ModuleType.SCHEDULING
    }
}

@NablaInternal
public interface SchedulingInternalClient {

    @NablaInternal
    public fun openScheduleAppointmentActivity(context: Context)
}
