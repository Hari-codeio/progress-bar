package app.becoming.domain

/** A preset identity icon. Pure data: the UI layer maps [key] to a drawable. */
data class IdentityIcon(val key: String, val label: String, val keywords: List<String>)

object IdentityIcons {
    val ALL: List<IdentityIcon> = listOf(
        IdentityIcon(
            "gym", "Gym",
            listOf("gym", "fitness", "workout", "weights", "weightlifting", "lifting", "lifter", "strength",
                "bodybuilding", "bodybuilder", "crossfit", "powerlifting", "calisthenics"),
        ),
        IdentityIcon(
            "athlete", "Runner",
            listOf("run", "runner", "running", "marathon", "jog", "jogger", "jogging", "athlete", "athletics",
                "sprint", "sprinter", "walk", "walker", "walking", "hike", "hiker", "hiking", "triathlete", "triathlon"),
        ),
        IdentityIcon("badminton", "Badminton", listOf("badminton", "shuttle", "shuttler", "shuttlecock")),
        IdentityIcon(
            "cricket", "Cricket",
            listOf("cricket", "cricketer", "batsman", "batter", "batting", "bowler", "wicketkeeper"),
        ),
        IdentityIcon("football", "Football", listOf("football", "footballer", "soccer", "futsal")),
        IdentityIcon(
            "tennis", "Tennis",
            listOf("tennis", "squash", "pickleball", "padel", "ping pong", "racket", "racquet"),
        ),
        IdentityIcon(
            "swimming", "Swimming",
            listOf("swim", "swimmer", "swimming", "pool", "diving", "diver", "freediving", "surfing", "surfer"),
        ),
        IdentityIcon(
            "cycling", "Cycling",
            listOf("cycling", "cyclist", "cycle", "bike", "biking", "biker", "bicycle", "mtb"),
        ),
        IdentityIcon("yoga", "Yoga", listOf("yoga", "yogi", "pilates", "stretching", "stretch", "mobility")),
        IdentityIcon(
            "meditation", "Meditation",
            listOf("meditation", "meditate", "meditator", "mindfulness", "mindful", "breathwork", "breathing",
                "calm", "gratitude", "prayer"),
        ),
        IdentityIcon(
            "developer", "Developer",
            listOf("code", "coding", "coder", "developer", "programmer", "programming", "software", "engineer",
                "engineering", "dev", "kotlin", "android", "python", "java", "javascript", "typescript", "web",
                "hacker", "algorithms"),
        ),
        IdentityIcon(
            "reader", "Reader",
            listOf("read", "reading", "reader", "book", "bookworm", "literature", "novels"),
        ),
        IdentityIcon(
            "writer", "Writer",
            listOf("write", "writing", "writer", "author", "journal", "journaling", "blog", "blogger", "blogging",
                "poet", "poetry", "novelist", "copywriter", "copywriting", "screenwriter", "storyteller"),
        ),
        IdentityIcon(
            "musician", "Musician",
            listOf("music", "musician", "guitar", "guitarist", "piano", "pianist", "violin", "violinist", "drum",
                "drummer", "drumming", "bass", "bassist", "ukulele", "flute", "flautist", "cello", "cellist",
                "saxophone", "composer", "producer", "dj"),
        ),
        IdentityIcon(
            "singer", "Singer",
            listOf("sing", "singing", "singer", "vocal", "vocalist", "choir", "karaoke", "rapper", "rap"),
        ),
        IdentityIcon(
            "artist", "Artist",
            listOf("art", "artist", "draw", "drawing", "paint", "painting", "painter", "sketch", "sketching",
                "illustration", "illustrator", "design", "designer", "calligraphy", "watercolor", "watercolour"),
        ),
        IdentityIcon(
            "photographer", "Photographer",
            listOf("photo", "photography", "photographer", "camera", "filmmaker", "filmmaking", "videography",
                "videographer"),
        ),
        IdentityIcon(
            "chef", "Chef",
            listOf("cook", "cooking", "chef", "bake", "baker", "baking", "kitchen", "cuisine", "culinary"),
        ),
        IdentityIcon(
            "student", "Student",
            listOf("study", "studies", "studying", "student", "exam", "school", "college", "university",
                "homework", "revision", "math", "maths", "mathematics", "science", "physics", "chemistry", "biology"),
        ),
        IdentityIcon(
            "language", "Language learner",
            listOf("language", "linguist", "polyglot", "spanish", "french", "german", "japanese", "mandarin",
                "chinese", "korean", "hindi", "tamil", "telugu", "malayalam", "kannada", "arabic", "italian",
                "portuguese", "russian", "english", "duolingo", "vocabulary", "translate", "translator"),
        ),
        IdentityIcon(
            "entrepreneur", "Entrepreneur",
            listOf("entrepreneur", "founder", "startup", "business", "businessman", "businesswoman", "ceo",
                "sales", "marketing", "marketer", "investor", "investing", "freelancer"),
        ),
        IdentityIcon(
            "speaker", "Speaker",
            listOf("speak", "speaking", "speaker", "public speaking", "speech", "presenter", "presenting",
                "presentation", "toastmasters", "debate", "debater", "podcast", "podcaster", "storytelling"),
        ),
    )

    private val index: Map<String, IdentityIcon> = ALL.associateBy { it.key }

    fun byKey(key: String?): IdentityIcon? = key?.let { index[it] }
}

/**
 * Suggests a preset for what the user typed, or null when nothing fits. Case-insensitive, whole-word
 * match (a trailing plural "s" is ignored); multi-word keywords match as phrases. The skill is checked
 * before the identity title, and presets are tried in [IdentityIcons.ALL] order.
 */
fun suggestIconKey(skill: String, identityTitle: String): String? {
    for (text in listOf(skill, identityTitle)) {
        val words = words(text)
        if (words.isEmpty()) continue
        val phrase = words.joinToString(" ")
        val match = IdentityIcons.ALL.firstOrNull { icon ->
            icon.keywords.any { keyword ->
                if (' ' in keyword) " $phrase ".contains(" $keyword ") else words.any { it == keyword || it == keyword + "s" }
            }
        }
        if (match != null) return match.key
    }
    return null
}

/** The icon to show: a valid stored preset, otherwise the suggestion (null means the default mark). */
fun resolveIconKey(storedKey: String?, skill: String, identityTitle: String): String? =
    IdentityIcons.byKey(storedKey)?.key ?: suggestIconKey(skill, identityTitle)

/** Lower-case words made of letters and digits. */
private fun words(text: String): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    for (ch in text.lowercase()) {
        if (ch.isLetterOrDigit()) {
            current.append(ch)
        } else if (current.isNotEmpty()) {
            result.add(current.toString())
            current.clear()
        }
    }
    if (current.isNotEmpty()) result.add(current.toString())
    return result
}
