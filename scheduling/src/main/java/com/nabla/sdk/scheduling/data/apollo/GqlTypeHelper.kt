package com.nabla.sdk.scheduling.data.apollo

import com.nabla.sdk.scheduling.graphql.PastAppointmentsQuery
import com.nabla.sdk.scheduling.graphql.UpcomingAppointmentsQuery
import com.nabla.sdk.scheduling.graphql.fragment.AppointmentsPageFragment

internal object GqlTypeHelper {
    fun UpcomingAppointmentsQuery.Data.modify(
        appointmentsPage: AppointmentsPageFragment,
    ): UpcomingAppointmentsQuery.Data {
        return copy(
            upcomingAppointments = this.upcomingAppointments.copy(
                appointmentsPageFragment = appointmentsPage
            )
        )
    }

    fun PastAppointmentsQuery.Data.modify(
        appointmentsPage: AppointmentsPageFragment,
    ): PastAppointmentsQuery.Data {
        return copy(
            pastAppointments = this.pastAppointments.copy(
                appointmentsPageFragment = appointmentsPage
            )
        )
    }
}
