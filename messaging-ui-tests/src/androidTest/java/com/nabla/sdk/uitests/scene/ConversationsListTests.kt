package com.nabla.sdk.uitests.scene

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.uitests.first
import com.nabla.sdk.uitests.withCousin
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.nabla.sdk.messaging.ui.R as SdkR

@RunWith(AndroidJUnit4::class)
class ConversationsListTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun conversations_list_unread_and_scroll_test() {
        // verify unread messages dot is shown/hidden properly
        onView(
            allOf(
                withCousin(SdkR.id.conversationListViewItemRoot, withText("With Unreads")),
                withId(SdkR.id.unreadDot),
            ),
        ).check(matches(isDisplayed()))
        onView(
            allOf(
                withCousin(SdkR.id.conversationListViewItemRoot, withText("Without Unreads")),
                withId(SdkR.id.unreadDot),
            ),
        ).check(matches(not(isDisplayed())))

        var varAdapter: RecyclerView.Adapter<*>? = null
        activityRule.scenario.onActivity {
            varAdapter = it.findViewById<RecyclerView>(SdkR.id.conversationsRecyclerView).adapter!!
        }
        val adapter = varAdapter!!

        val itemCountBeforeScroll = adapter.itemCount
        onView(first(withId(SdkR.id.conversationsRecyclerView))).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                withId(SdkR.id.nablaConversationItemLoadingMoreRoot),
            ),
        )
        // TODO find a way to make Espresso wait for recycler‘s adapter submitList.
        Thread.sleep(100)

        val newItemCount = adapter.itemCount

        assert(newItemCount > itemCountBeforeScroll) {
            "Scroll to paginate did not increase number of items: $itemCountBeforeScroll became $newItemCount"
        }
    }
}
