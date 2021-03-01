package com.example.vinmod;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DiscussionHomeTests{

    @Rule
    public ActivityTestRule<Discussion> activityRule =
            new ActivityTestRule<Discussion>(Discussion.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void newPostClickTest(){
        onView(withId(R.id.newPost_Btn)).perform(ViewActions.click());
        intended(hasComponent(NewPost.class.getName()));
    }

    @Test
    public void onPostClickTest(){
        onView(withId(R.id.postName)).perform(ViewActions.click());
        intended(hasComponent(ViewPost.class.getName()));
    }

    @Test
    public void sortByNewTest(){
        onView(withId(R.id.sort_options)).perform(ViewActions.click());
        onView(allOf(withId(text1), withText("Date(newest)"))).perform(ViewActions.click());
        Discussion discussionSnapshot = activityRule.getActivity();
        String firstPostDate = discussionSnapshot.discussionPosts.get(0).getDate();
        firstPostDate = firstPostDate.substring(6, 10) + firstPostDate.substring(3, 5) + firstPostDate.substring(0, 2) + firstPostDate.substring(14).replace(":", "");
        String lastPostDate = discussionSnapshot.discussionPosts.get(0).getDate();
        lastPostDate = lastPostDate.substring(6, 10) + lastPostDate.substring(3, 5) + lastPostDate.substring(0, 2) + lastPostDate.substring(14).replace(":", "");
        int firstPostDateValue = Integer.parseInt(firstPostDate);
        int lastPostDateValue = Integer.parseInt(lastPostDate);
        Assert.assertTrue(firstPostDateValue > lastPostDateValue);
    }

    @Test
    public void sortByOldTest(){
        onView(withId(R.id.sort_options)).perform(ViewActions.click());
        onView(allOf(withId(text1), withText("Date(oldest)"))).perform(ViewActions.click());
        Discussion discussionSnapshot = activityRule.getActivity();
        String firstPostDate = discussionSnapshot.discussionPosts.get(0).getDate();
        firstPostDate = firstPostDate.substring(6, 10) + firstPostDate.substring(3, 5) + firstPostDate.substring(0, 2) + firstPostDate.substring(14).replace(":", "");
        String lastPostDate = discussionSnapshot.discussionPosts.get(0).getDate();
        lastPostDate = lastPostDate.substring(6, 10) + lastPostDate.substring(3, 5) + lastPostDate.substring(0, 2) + lastPostDate.substring(14).replace(":", "");
        int firstPostDateValue = Integer.parseInt(firstPostDate);
        int lastPostDateValue = Integer.parseInt(lastPostDate);
        Assert.assertTrue(firstPostDateValue < lastPostDateValue);
    }

    @Test
    public void sortByPopTest(){
        onView(withId(R.id.sort_options)).perform(ViewActions.click());
        onView(allOf(withId(text1), withText("Most Popular"))).perform(ViewActions.click());
        Discussion discussionSnapshot = activityRule.getActivity();
        String firstPostReplies = discussionSnapshot.discussionPosts.get(0).getReplyCount();
        String lastPostReplies = discussionSnapshot.discussionPosts.get(0).getReplyCount();
        Assert.assertTrue(firstPostReplies > lastPostReplies);
    }

}
