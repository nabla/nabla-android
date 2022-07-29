package com.nabla.sdk.uitests.scene

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.longClick
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.nabla.sdk.uitests.createImageGallerySetResultStub
import com.nabla.sdk.uitests.first
import com.nabla.sdk.uitests.savePickedImage
import com.nabla.sdk.uitests.withCousin
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import com.nabla.sdk.messaging.ui.R as SdkR

@RunWith(AndroidJUnit4::class)
class MessageSendingTests {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        Intents.init()
        // save image to device storage to be used later in media sending
        savePickedImage(context)
    }

    @Test @Ignore("issues/20984")
    fun create_empty_conversation_and_send_messages_test() {
        // create new conversation & open it
        onView(first(withId(SdkR.id.createConversationCta))).perform(click())

        onView(withId(SdkR.id.conversationToolbarTitle)).check(matches(withText("New conversation")))
        onView(withId(SdkR.id.conversationEditText)).check(matches(withText("")))
        onView(withId(SdkR.id.conversationSendButton)).check(matches(not(isEnabled())))
        activityRule.scenario.onActivity {
            assertEquals(
                expected = 0,
                it.findViewById<RecyclerView>(SdkR.id.conversationRecyclerView).adapter!!.itemCount,
                "Expected recycler view to be empty on freshly created conversation",
            )
        }

        // write some text
        onView(withId(SdkR.id.conversationEditText)).perform(typeText("Hello!"))
        onView(withId(SdkR.id.conversationSendButton)).check(matches(isEnabled()))
        assert(nablaMessagingClientStub.isTyping) { "started typing on conversation composer but patient was not marked as typing" }

        // send it
        onView(withId(SdkR.id.conversationSendButton)).perform(click())
        onView(allOf(withId(SdkR.id.chatTextMessageTextView), withText("Hello!"))).check(matches(isDisplayed()))
        onView(
            allOf(
                withId(SdkR.id.chatPatientMessageContentStatusTextView),
                withText(context.getString(SdkR.string.nabla_conversation_message_sending_status))
            )
        ).check(matches(not(isDisplayed())))

        // send a failing message
        val failingMessage = "Please fail first attempt of this message."
        onView(withId(SdkR.id.conversationEditText)).perform(typeText(failingMessage))
        onView(withId(SdkR.id.conversationSendButton)).perform(click())
        onView(allOf(withId(SdkR.id.chatTextMessageTextView), withText(failingMessage))).check(matches(isDisplayed()))
        onView(
            allOf(
                withId(SdkR.id.chatPatientMessageContentStatusTextView),
                withText(context.getString(SdkR.string.nabla_conversation_message_error_status)),
            )
        ).check(matches(isDisplayed()))

        // retry it
        onView(withText(failingMessage)).perform(click())
        onView(
            allOf(
                withId(SdkR.id.chatTextMessageTextView),
                withText(failingMessage),
            )
        ).check(
            matches(
                // has a hidden status = was sent successfully
                withCousin(SdkR.id.chatPatientMessageContentRoot, allOf(withId(SdkR.id.chatPatientMessageContentStatusTextView), not(isDisplayed()))),
            )
        )

        // delete it
        onView(withText(failingMessage)).perform(longClick())
        onView(withText(context.getString(SdkR.string.nabla_conversation_message_action_delete))).perform(click())
        onView(withText(failingMessage)).check(doesNotExist())
        onView(withText(context.getString(SdkR.string.nabla_conversation_deleted_message_placeholder))).check(matches(isDisplayed()))

        // test typing indicator
        onView(withId(SdkR.id.conversationEditText)).perform(typeText("Please reply"))
        onView(withId(SdkR.id.conversationSendButton)).perform(click())
        onView(withId(SdkR.id.dots)).check(matches(isDisplayed()))

        // send gallery image message
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(createImageGallerySetResultStub(context))
        onView(withId(SdkR.id.conversationAddMediaButton)).perform(click())
        onView(withId(SdkR.id.chatMediaSourcePickerOptionLibrary)).perform(click())
        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))

        onView(withId(SdkR.id.conversationSendButton)).perform(click())

        // TODO add idling resource for Coil ImageLoader (abstract it first)
        Thread.sleep(5_000)

        onView(withId(SdkR.id.nablaConversationTimelineItemImageRoot)).check(matches(isDisplayed()))

        // Record & send audio message
        onView(withId(SdkR.id.conversationRecordVoiceButton)).perform(click())
        Thread.sleep(1_100) // recording time
        onView(withId(SdkR.id.conversationSendButton)).perform(click())
        onView(allOf(withId(SdkR.id.audioMessageSecondsText), withText("00:01"))).check(matches(isDisplayed()))
        onView(withId(SdkR.id.audioPlayPauseButton)).perform(click())
    }
}
