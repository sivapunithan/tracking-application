package com.spcode.tnpscprep.domain

/** Current-affairs categories (stored by NAME in the pack and in Room, so never rename; only add). */
enum class AffairCategory {
    POLITY, TN, INDIA, WORLD, ECONOMY, SCIENCE, SPORTS, AWARDS;

    companion object {
        fun fromName(name: String?): AffairCategory? = entries.firstOrNull { it.name == name }
    }
}
