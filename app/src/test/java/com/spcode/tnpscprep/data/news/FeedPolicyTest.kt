package com.spcode.tnpscprep.data.news

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedPolicyTest {
    private val hosts = setOf("thehindu.com", "feeds.bbci.co.uk")

    @Test fun allows_https_on_listed_hosts_and_their_subdomains() {
        assertTrue(FeedPolicy.isAllowed("https://www.thehindu.com/news/national/feeder/default.rss", hosts))
        assertTrue(FeedPolicy.isAllowed("https://thehindu.com/x", hosts))
        assertTrue(FeedPolicy.isAllowed("https://feeds.bbci.co.uk/tamil/rss.xml", hosts))
    }

    @Test fun rejects_plain_http_and_other_schemes() {
        assertFalse(FeedPolicy.isAllowed("http://www.thehindu.com/x", hosts))
        assertFalse(FeedPolicy.isAllowed("ftp://www.thehindu.com/x", hosts))
        assertFalse(FeedPolicy.isAllowed("javascript:alert(1)", hosts))
        assertFalse(FeedPolicy.isAllowed("file:///etc/passwd", hosts))
    }

    @Test fun rejects_look_alike_and_trick_hosts() {
        assertFalse(FeedPolicy.isAllowed("https://evilthehindu.com/x", hosts))
        assertFalse(FeedPolicy.isAllowed("https://thehindu.com.evil.org/x", hosts))
        assertFalse(FeedPolicy.isAllowed("https://thehindu.com@evil.org/x", hosts)) // real host is evil.org
        assertFalse(FeedPolicy.isAllowed("https://evil.org/?u=https://thehindu.com", hosts))
    }

    @Test fun garbage_urls_are_rejected_not_thrown() {
        assertFalse(FeedPolicy.isAllowed("not a url", hosts))
        assertFalse(FeedPolicy.isAllowed("", hosts))
    }

    @Test fun host_matching_ignores_case() = assertTrue(FeedPolicy.isAllowed("https://WWW.TheHindu.COM/x", hosts))
}
