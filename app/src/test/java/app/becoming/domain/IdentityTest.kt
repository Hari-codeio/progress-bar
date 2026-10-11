package app.becoming.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityTest {

    // ---- validateIdentity ------------------------------------------------------------------

    @Test
    fun validationTrimsAndCollapsesWhitespace() {
        val v = validateIdentity("  Sam  ", " Table   tennis ", "Table\t tennis \n Player")
        assertTrue(v.isValid)
        assertEquals("Sam", v.name)
        assertEquals("Table tennis", v.skill)
        assertEquals("Table tennis Player", v.identityTitle)
        assertEquals("a b c", normalizeIdentityField("\ta   b\n\nc "))
        assertEquals("", normalizeIdentityField("   \t "))
    }

    @Test
    fun validationFlagsEmptyFieldsIndependently() {
        val v = validateIdentity("   ", "Badminton", "")
        assertFalse(v.isValid)
        assertEquals(FieldError.EMPTY, v.nameError)
        assertNull(v.skillError)
        assertEquals(FieldError.EMPTY, v.identityTitleError)
    }

    @Test
    fun validationLimitsAre30_30_32() {
        val ok = validateIdentity("n".repeat(30), "s".repeat(30), "t".repeat(32))
        assertTrue(ok.isValid)
        val tooLong = validateIdentity("n".repeat(31), "s".repeat(31), "t".repeat(33))
        assertEquals(FieldError.TOO_LONG, tooLong.nameError)
        assertEquals(FieldError.TOO_LONG, tooLong.skillError)
        assertEquals(FieldError.TOO_LONG, tooLong.identityTitleError)
        // The limit applies after collapsing: long runs of spaces don't count against it.
        assertTrue(validateIdentity("Sam", "a" + " ".repeat(40) + "b", "Player").isValid)
        assertEquals(30, IdentityLimits.NAME_MAX)
        assertEquals(30, IdentityLimits.SKILL_MAX)
        assertEquals(32, IdentityLimits.TITLE_MAX)
    }

    // ---- Icon presets and suggestions ------------------------------------------------------

    @Test
    fun twentyTwoUniquePresets() {
        val keys = IdentityIcons.ALL.map { it.key }
        assertEquals(22, keys.size)
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(
            listOf(
                "gym", "athlete", "badminton", "cricket", "football", "tennis", "swimming", "cycling", "yoga",
                "meditation", "developer", "reader", "writer", "musician", "singer", "artist", "photographer",
                "chef", "student", "language", "entrepreneur", "speaker",
            ),
            keys,
        )
        assertTrue(IdentityIcons.ALL.all { it.label.isNotBlank() && it.keywords.isNotEmpty() })
        val allKeywords = IdentityIcons.ALL.flatMap { it.keywords }
        assertEquals("a keyword must belong to one preset only", allKeywords.size, allKeywords.toSet().size)
        assertEquals("Badminton", IdentityIcons.byKey("badminton")?.label)
        assertNull(IdentityIcons.byKey("unknown"))
        assertNull(IdentityIcons.byKey(null))
    }

    @Test
    fun suggestionMatchesKeywordsCaseInsensitively() {
        assertEquals("badminton", suggestIconKey("Badminton", "Badminton Player"))
        assertEquals("badminton", suggestIconKey("Shuttle", ""))
        assertEquals("gym", suggestIconKey("Gym", ""))
        assertEquals("gym", suggestIconKey("", "Fitness Freak"))
        assertEquals("gym", suggestIconKey("WORKOUT", "Warrior"))
        assertEquals("developer", suggestIconKey("Coding", "Developer"))
        assertEquals("developer", suggestIconKey("code", ""))
        assertEquals("developer", suggestIconKey("Kotlin", "Programmer"))
        assertEquals("athlete", suggestIconKey("", "Marathon Runner"))
        assertEquals("reader", suggestIconKey("Books", "")) // plural
        assertEquals("tennis", suggestIconKey("Table tennis", ""))
        assertEquals("tennis", suggestIconKey("Ping pong", "")) // phrase keyword
        assertEquals("musician", suggestIconKey("Piano", "Pianist"))
    }

    @Test
    fun suggestionPrefersSkillThenTitle() {
        assertEquals("singer", suggestIconKey("Singing", "Singer Songwriter"))
        assertEquals("language", suggestIconKey("Learn Spanish", "Spanish Speaker"))
        assertEquals("speaker", suggestIconKey("Public speaking", "Speaker"))
        assertEquals("language", suggestIconKey("", "Hindi Learner"))
    }

    @Test
    fun suggestionMatchesWholeWordsOnly() {
        assertNull(suggestIconKey("Knitting", "Maker"))
        assertNull(suggestIconKey("Already ready", "Steady")) // "read" must not match "ready"
        assertNull(suggestIconKey("", ""))
    }

    @Test
    fun resolveIconKeyPrefersAValidStoredPreset() {
        assertEquals("chef", resolveIconKey("chef", "Badminton", "Badminton Player"))
        assertEquals("badminton", resolveIconKey(null, "Badminton", "Badminton Player"))
        assertEquals("badminton", resolveIconKey("not-a-preset", "Badminton", "Badminton Player"))
        assertNull(resolveIconKey(null, "Knitting", "Maker"))
    }

    @Test
    fun homeStateSuggestsAnIconForOneZeroUsers() {
        // 1.0.0 journeys migrate with iconKey = iconPath = NULL.
        val journey = JourneyInput(1L, "Sam", "Badminton", "Badminton Player", 100L, 100L, 1)
        val s = HabitRules.homeState(journey, listOf(100L), 100L, GreetBy.IDENTITY)
        assertNull(s.iconKey)
        assertNull(s.iconPath)
        assertEquals("badminton", s.displayIconKey)

        val picked = journey.copy(iconKey = "athlete", iconPath = "identity/journey_1_5.png")
        val p = HabitRules.homeState(picked, listOf(100L), 100L, GreetBy.IDENTITY)
        assertEquals("athlete", p.displayIconKey)
        assertEquals("identity/journey_1_5.png", p.iconPath)
    }

    // ---- Dark mode -------------------------------------------------------------------------

    @Test
    fun resolveDarkFollowsSystemUnlessForced() {
        assertTrue(resolveDark(null, systemDark = true))
        assertFalse(resolveDark(null, systemDark = false))
        assertTrue(resolveDark(true, systemDark = false))
        assertTrue(resolveDark(true, systemDark = true))
        assertFalse(resolveDark(false, systemDark = true))
        assertFalse(resolveDark(false, systemDark = false))
    }
}
