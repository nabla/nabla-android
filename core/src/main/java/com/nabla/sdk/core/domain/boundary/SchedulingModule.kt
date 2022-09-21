package com.nabla.sdk.core.domain.boundary

import android.content.Context
import com.nabla.sdk.core.domain.entity.ModuleType

public interface SchedulingModule : Module {
    public fun openScheduleAppointmentActivity(context: Context)

    public fun interface Factory : Module.Factory<SchedulingModule> {
        override fun type(): ModuleType = ModuleType.SCHEDULING
    }
}
