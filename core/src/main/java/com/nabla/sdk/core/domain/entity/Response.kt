package com.nabla.sdk.core.domain.entity

/**
 * A response from a watcher that can emit cached data for optimistic UI or offline access.
 *
 * This object exposes:
 * - [isDataFresh] that gives an indicator of the freshness of the data. It will be true if the response comes
 * from a network call or if it's a local only object
 * - [refreshingState] that exposes any request being made in background to refresh the produced data
 *
 * When calling the watcher, if some cached data is available, a [Response] will be emitted first with
 * the cache data (meaning [isDataFresh] will be true) and a background network request will start
 * to refresh the cached data (meaning [refreshingState] will be [RefreshingState.Refreshing]). If that
 * request succeeds, a new [Response] will be emitted with [isDataFresh] set to true and [refreshingState]
 * set to [RefreshingState.Refreshed]. If that request fails, a new [Response] will be emitted with
 * [isDataFresh] set to false and [refreshingState] set to [RefreshingState.ErrorWhileRefreshing].
 *
 * When calling the watcher if no cached data is available, the flow won't emit any [Response] until the network
 * request finishes. If this request succeeds, a new [Response] will be emitted with [isDataFresh] set to true
 * and [refreshingState] set to [RefreshingState.Refreshed]. If that request fails, the watcher will
 * throw an error that you should use using `catch`.
 */
public data class Response<T>(
    val isDataFresh: Boolean,
    val refreshingState: RefreshingState,
    val data: T,
)

public sealed class RefreshingState {
    /**
     * The refresh request did succeed
     */
    public object Refreshed : RefreshingState()

    /**
     * A request to refresh the data is being done in background
     */
    public object Refreshing : RefreshingState()

    /**
     * An error occurred during the refresh request.
     */
    public data class ErrorWhileRefreshing(val error: NablaException) : RefreshingState()
}
