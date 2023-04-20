package com.nabla.sdk.uitests.scene

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nabla.sdk.uitests.first
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.nabla.sdk.messaging.ui.R as SdkR

@RunWith(AndroidJUnit4::class)
class MessagesListTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun scroll_messages_and_assert_pagination_test() {
        // open first conversation
        onView(first(allOf(withId(SdkR.id.conversationInboxTitle), withText("With Unreads")))).perform(click())

        var varAdapter: RecyclerView.Adapter<*>? = null
        activityRule.scenario.onActivity {
            varAdapter = it.findViewById<RecyclerView>(SdkR.id.conversationRecyclerView).adapter!!
        }
        val adapter = varAdapter!!

        val itemCountBeforeScroll = adapter.itemCount

        // scroll till pagination trigger
        onView(first(withId(SdkR.id.conversationRecyclerView))).perform(
            RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                withId(SdkR.id.nablaConversationTimelineLoadingMoreRoot),
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
