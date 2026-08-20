package com.safenest.kids.util

/**
 * Resolves the name the Home greeting addresses the child by.
 *
 * The greeting text itself lives in `strings.xml`; this object only decides whether a usable name
 * exists, so the caller picks between the named and the neutral string resource.
 */
object ChildGreeting {

    /** Long enough for a real given name, short enough that the header never wraps. */
    const val MAX_DISPLAY_LENGTH = 18

    private val WHITESPACE = Regex("\\s+")

    /**
     * Returns the name to place in the greeting, or `null` when the Backend has not supplied a
     * usable one and the caller should fall back to the neutral phrase.
     */
    fun displayName(rawName: String?): String? {
        if (rawName == null) return null
        // A profile saved with only spaces, or with a stray newline from a paste, must not produce
        // a greeting that renders as "أهلًا !" or breaks across two lines. A control character
        // becomes a space before the collapse rather than being dropped, so a two-word name split
        // across lines stays two words instead of being welded into one.
        val collapsed = rawName
            .map { if (it.isISOControl()) ' ' else it }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()
        if (collapsed.isEmpty()) return null
        if (collapsed.length <= MAX_DISPLAY_LENGTH) return collapsed
        return collapsed.take(MAX_DISPLAY_LENGTH).trimEnd() + "…"
    }
}
