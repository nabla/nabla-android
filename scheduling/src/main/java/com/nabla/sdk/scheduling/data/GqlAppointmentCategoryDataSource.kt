package com.nabla.sdk.scheduling.data

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.watch
import com.nabla.sdk.core.data.apollo.CacheUpdateOperation
import com.nabla.sdk.core.data.apollo.dataOrThrowOnError
import com.nabla.sdk.core.data.apollo.updateCache
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.scheduling.data.apollo.GqlMapper
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.graphql.AppointmentCategoriesQuery
import com.nabla.sdk.scheduling.graphql.AvailabilitySlotsQuery
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

internal class GqlAppointmentCategoryDataSource(
    private val apolloClient: ApolloClient,
    private val mapper: GqlMapper,
) {
    suspend fun getCategories() = apolloClient.query(AppointmentCategoriesQuery())
        .fetchPolicy(FetchPolicy.NetworkFirst)
        .execute()
        .dataOrThrowOnError
        .appointmentCategories
        .categories
        .map { mapper.mapToAppointmentCategory(it.appointmentCategoryFragment) }

    fun watchAvailabilitySlots(categoryId: AppointmentCategoryId): Flow<PaginatedList<AvailabilitySlot>> {
        return apolloClient.query(availabilitySlotsPageQuery(categoryId))
            .fetchPolicy(FetchPolicy.NetworkFirst)
            .watch(fetchThrows = true)
            .map { response -> response.dataOrThrowOnError }
            .map { queryData ->
                val page = queryData.appointmentCategory.category.availableSlots.availabilitySlotsPageFragment
                val slots = page.slots
                    .filter { Clock.System.now() < it.availabilitySlotFragment.startAt }
                    .map { slot -> mapper.mapToAvailabilitySlot(slot.availabilitySlotFragment) }

                PaginatedList(slots, page.hasMore)
            }
    }

    suspend fun loadMoreAvailabilitySlots(categoryId: AppointmentCategoryId) {
        apolloClient.updateCache(availabilitySlotsPageQuery(categoryId)) { cachedQueryData ->
            val cachedPages = cachedQueryData?.appointmentCategory?.category?.availableSlots?.availabilitySlotsPageFragment
            if (cachedPages == null || !cachedPages.hasMore) {
                return@updateCache CacheUpdateOperation.Ignore()
            }
            val freshQueryData = apolloClient
                .query(availabilitySlotsPageQuery(categoryId, cachedPages.nextCursor))
                .fetchPolicy(FetchPolicy.NetworkOnly)
                .execute()
                .dataOrThrowOnError
            val newPage = freshQueryData.appointmentCategory.category.availableSlots.availabilitySlotsPageFragment

            CacheUpdateOperation.Write(
                freshQueryData.copy(
                    appointmentCategory = freshQueryData.appointmentCategory.copy(
                        category = freshQueryData.appointmentCategory.category.copy(
                            availableSlots = freshQueryData.appointmentCategory.category.availableSlots.copy(
                                availabilitySlotsPageFragment = newPage.copy(
                                    slots = cachedPages.slots + newPage.slots
                                )
                            )
                        )
                    )
                )
            )
        }
    }

    suspend fun resetAvailabilitySlotsCache(categoryId: AppointmentCategoryId) {
        apolloClient.query(availabilitySlotsPageQuery(categoryId))
            .fetchPolicy(FetchPolicy.NetworkOnly)
            .execute()
    }

    companion object {
        private fun availabilitySlotsPageQuery(categoryId: AppointmentCategoryId, cursorPage: String? = null) = AvailabilitySlotsQuery(
            categoryId.value,
            OpaqueCursorPage(cursor = Optional.presentIfNotNull(cursorPage), numberOfItems = Optional.Present(100))
        )
    }
}
