package dev.typetype.android.data.network

import dev.typetype.android.data.network.dto.SubscriptionGroupDto
import dev.typetype.android.data.network.dto.SubscriptionGroupMembershipDto
import dev.typetype.android.data.network.dto.SubscriptionGroupMembershipRequest
import dev.typetype.android.data.network.dto.SubscriptionGroupNameRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TypeTypeSubscriptionGroupsApi {
    @GET("subscriptions/groups")
    suspend fun subscriptionGroups(): Response<List<SubscriptionGroupDto>>

    @POST("subscriptions/groups")
    suspend fun createSubscriptionGroup(@Body body: SubscriptionGroupNameRequest): Response<SubscriptionGroupDto>

    @PUT("subscriptions/groups/{groupId}")
    suspend fun renameSubscriptionGroup(
        @Path("groupId") groupId: String,
        @Body body: SubscriptionGroupNameRequest,
    ): Response<Unit>

    @DELETE("subscriptions/groups/{groupId}")
    suspend fun deleteSubscriptionGroup(@Path("groupId") groupId: String): Response<Unit>

    @PUT("subscriptions/groups/{groupId}/channels")
    suspend fun addSubscriptionGroupChannel(
        @Path("groupId") groupId: String,
        @Body body: SubscriptionGroupMembershipRequest,
    ): Response<Unit>

    @DELETE("subscriptions/groups/{groupId}/channels")
    suspend fun removeSubscriptionGroupChannel(
        @Path("groupId") groupId: String,
        @Query("url") channelUrl: String,
    ): Response<Unit>

    @POST("subscriptions/group-memberships/lookup")
    suspend fun lookupSubscriptionGroupMemberships(
        @Body body: SubscriptionGroupMembershipRequest,
    ): Response<List<SubscriptionGroupMembershipDto>>
}
