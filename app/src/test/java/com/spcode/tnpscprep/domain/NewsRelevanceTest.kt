package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsRelevanceTest {
    private val india = AffairCategory.INDIA

    @Test fun exam_style_headlines_score_high_and_get_a_category() {
        assertEquals(AffairCategory.POLITY, NewsRelevance.category("Supreme Court upholds validity of SIR", india))
        assertEquals(AffairCategory.ECONOMY, NewsRelevance.category("RBI keeps repo rate unchanged", india))
        assertEquals(AffairCategory.SCIENCE, NewsRelevance.category("ISRO to launch navigation satellite", india))
        assertEquals(AffairCategory.TN, NewsRelevance.category("Chennai Metro Phase II gets approval", india))
        assertTrue(NewsRelevance.score("Supreme Court upholds validity of SIR", india) >= 2)
    }

    @Test fun entertainment_and_unrelated_headlines_are_dropped() {
        assertTrue(NewsRelevance.score("Box office: new movie trailer breaks records", india) < 2)
        assertTrue(NewsRelevance.score("Man finds snake in kitchen", india) < 2)
    }

    @Test fun a_feed_hint_is_kept_when_it_matches_a_keyword() {
        assertEquals(AffairCategory.ECONOMY, NewsRelevance.category("GST Council meets; Finance Minister speaks", AffairCategory.ECONOMY))
    }

    @Test fun specialised_feeds_get_a_bonus_so_one_generic_keyword_is_enough() {
        assertTrue(NewsRelevance.score("State launches new scheme", AffairCategory.TN) >= 2)
        assertTrue(NewsRelevance.score("State launches new scheme", india) < 2)
    }

    @Test fun tamil_headlines_are_recognised() {
        assertEquals(AffairCategory.POLITY, NewsRelevance.category("உச்ச நீதிமன்றம் முக்கிய தீர்ப்பு", india))
        assertTrue(NewsRelevance.score("தமிழ்நாடு அரசு புதிய அறிவிப்பு", india) >= 2)
        assertTrue(NewsRelevance.score("பிரபல நடிகர் புதிய திரைப்படம்", india) < 2)
    }

    @Test fun the_word_AI_is_matched_as_a_word_not_inside_other_words() {
        assertTrue(NewsRelevance.score("Rain delays Mumbai trains", india) < 2) // "AI" inside "Rain" must not match
    }

    @Test fun markets_shopping_and_street_crime_are_dropped() {
        assertTrue(NewsRelevance.score("Madurai showroom staff find 20kg gold stolen", AffairCategory.TN) < 2)
        assertTrue(NewsRelevance.score("Deepavali sales expected to gain momentum in Coimbatore", AffairCategory.ECONOMY) < 2)
        assertTrue(NewsRelevance.score("$2.5 trillion wiped off Nasdaq 100 as AI stocks slump", AffairCategory.ECONOMY) < 2)
        assertTrue(NewsRelevance.score("Daily Quiz: On the early Space Race", AffairCategory.SCIENCE) < 2)
    }

    @Test fun real_exam_style_headlines_still_pass_after_the_new_rules() {
        assertTrue(NewsRelevance.score("Union Cabinet approves VGF scheme to develop energy storage", AffairCategory.ECONOMY) >= 2)
        assertTrue(NewsRelevance.score("Madras High Court: MLAs cannot threaten officials", AffairCategory.TN) >= 2)
        assertTrue(NewsRelevance.score("Back to back GSLV successes help ISRO win back some pride", AffairCategory.SCIENCE) >= 2)
    }

    @Test fun a_sport_feed_keeps_sports_even_if_a_polity_word_appears() {
        assertEquals(AffairCategory.SPORTS, NewsRelevance.category("Dhoni ends IPL career as CSK appoints him team mentor", AffairCategory.SPORTS))
    }

    @Test fun a_specialised_feed_still_lets_a_clear_tamil_nadu_story_be_filed_under_tamil_nadu() {
        assertEquals(AffairCategory.TN, NewsRelevance.category("Chennai Metro gets approval", AffairCategory.ECONOMY))
    }

    @Test fun a_general_feed_uses_the_keywords() {
        assertEquals(AffairCategory.POLITY, NewsRelevance.category("CBI chief appointed", india))
    }
}
