package com.spcode.tnpscprep.domain

/**
 * Heuristic: decides whether a headline LOOKS exam-relevant and which category it belongs to.
 * It is keyword based, so it is imperfect on purpose-simple: it can miss items and let a few irrelevant ones through.
 * Weights: each matching keyword GROUP adds its weight once; "denied" groups (entertainment) subtract.
 */
object NewsRelevance {
    private class Group(val category: AffairCategory?, val weight: Int, pattern: String) {
        val regex = Regex(pattern, RegexOption.IGNORE_CASE)
    }

    private val groups = listOf(
        Group(AffairCategory.TN, 2, """\b(Tamil Nadu|TN govt|TNPSC|Chennai|Madurai|Coimbatore|Tiruchi|Tirunelveli|Salem|Madras High Court|Madras HC|Tamil)\b|தமிழ்நாடு|தமிழக|சென்னை|மதுரை|கோவை|திருச்சி|நெல்லை"""),
        Group(AffairCategory.POLITY, 2, """\b(Supreme Court|High Court|HC|Lok Sabha|Rajya Sabha|Parliament|Cabinet|Election Commission|sworn in|takes oath|appointed|appoints|takes charge|Governor|Speaker|Bill|Ordinance|Amendment|verdict|Constitution|delimitation|by-election|Assembly election|Chief Justice|Chief Minister|Prime Minister|President Murmu)\b|உச்ச நீதிமன்ற|உயர் நீதிமன்ற|நாடாளுமன்ற|மக்களவை|மாநிலங்களவை|அமைச்சரவை|தேர்தல்|முதல்வர்|ஆளுநர்|மசோதா|தீர்ப்பு|பிரதமர்|குடியரசுத் தலைவர்"""),
        Group(AffairCategory.ECONOMY, 2, """\b(RBI|Reserve Bank|repo rate|GDP|inflation|fiscal|Budget|GST|trade deal|free trade|FTA|tariff|Economic Survey|NITI Aayog|Finance Commission|Finance Minister|exports|imports|MSME|disinvestment|foreign exchange)\b|ரிசர்வ் வங்கி|பட்ஜெட்|ஜிஎஸ்டி|பணவீக்கம்|வர்த்தக ஒப்பந்த|பொருளாதார"""),
        Group(AffairCategory.SCIENCE, 2, """\b(ISRO|DRDO|satellite|missile|rocket|Gaganyaan|space|nuclear|Navy|Indian Army|Air Force|defence|vaccine|AI|artificial intelligence)\b|இஸ்ரோ|செயற்கைக்கோள்|ஏவுகணை|விண்வெளி|அணு"""),
        Group(AffairCategory.SPORTS, 2, """\b(World Cup|Olympic|Olympics|Asian Games|Commonwealth Games|Grand Slam|gold medal|champion|Test series|Ranji|BCCI|FIFA|ICC)\b|உலகக் கோப்பை|ஒலிம்பிக்|ஆசிய விளையாட்டு|தங்கப் பதக்க"""),
        Group(AffairCategory.AWARDS, 2, """\b(award|awarded|prize|Nobel|Padma|Bharat Ratna|UNESCO|honour|Sahitya Akademi|Jnanpith|Booker)\b|விருது|நோபல்|பரிசு"""),
        Group(AffairCategory.WORLD, 2, """\b(United Nations|UN|summit|treaty|ASEAN|BRICS|G20|Quad|bilateral|foreign minister|External Affairs|sanctions)\b|ஐ\.நா|ஐநா|உச்சி மாநாடு|ஒப்பந்தம்|தடைகள்"""),
        Group(null, 1, """\b(scheme|Yojana|Mission|policy|launches|inaugurates|MoU|agreement|recruitment|exam|notification)\b"""),
        // Markets, shopping and street crime read as "news" but are not exam material.
        Group(null, -3, """\b(Nasdaq|Sensex|Nifty|stock market|share price|stocks|IPO|quiz|stolen|theft|robbery|robbed|showroom|sales expected|discount|offer)\b"""),
        Group(null, -5, """\b(box office|movie|film review|trailer|OTT|web series|actor|actress|celebrity|Bigg Boss|serial|horoscope|wedding|reception)\b|திரைப்பட|சினிமா|நடிகர்|நடிகை|ஓடிடி"""),
    )

    /** Specialised feeds (Tamil Nadu, economy, science...) get +1 because everything in them is already on-topic. */
    private val hintBonus = setOf(AffairCategory.TN, AffairCategory.ECONOMY, AffairCategory.SCIENCE, AffairCategory.WORLD, AffairCategory.SPORTS)

    fun score(title: String, feedHint: AffairCategory): Int =
        groups.filter { it.regex.containsMatchIn(title) }.sumOf { it.weight } + if (feedHint in hintBonus) 1 else 0

    /**
     * A specialised feed (sport, science, economy, world, Tamil Nadu) already tells us the topic, so its hint wins;
     * the only override is a clear Tamil Nadu mention. On a general feed the first matching keyword group decides.
     */
    fun category(title: String, feedHint: AffairCategory): AffairCategory {
        val matched = groups.filter { it.category != null && it.regex.containsMatchIn(title) }.mapNotNull { it.category }
        return when {
            feedHint in hintBonus -> if (AffairCategory.TN in matched) AffairCategory.TN else feedHint
            matched.isNotEmpty() -> matched.first() // groups are listed in priority order
            else -> feedHint
        }
    }
}
