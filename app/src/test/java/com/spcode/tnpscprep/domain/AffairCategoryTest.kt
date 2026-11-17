package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AffairCategoryTest {
    @Test fun parses_known_names() = AffairCategory.entries.forEach { assertEquals(it, AffairCategory.fromName(it.name)) }
    @Test fun unknown_or_null_is_null() { assertNull(AffairCategory.fromName("sports")); assertNull(AffairCategory.fromName(null)) }
}
