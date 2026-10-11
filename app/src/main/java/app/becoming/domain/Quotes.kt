package app.becoming.domain

/**
 * Daily quotes from James Clear, *Atomic Habits*.
 * Verify each line word-for-word against the book before a public release, and keep the set small
 * and attributed (see README, "Quotes").
 */
val QUOTES: List<String> = listOf(
    "Every action you take is a vote for the type of person you wish to become.",
    "You do not rise to the level of your goals. You fall to the level of your systems.",
    "Habits are the compound interest of self-improvement.",
    "Missing once is an accident. Missing twice is the start of a new habit.",
    "Success is the product of daily habits—not once-in-a-lifetime transformations.",
    "All big things come from small beginnings.",
    "Your outcomes are a lagging measure of your habits.",
    "Time magnifies the margin between success and failure.",
    "A habit must be established before it can be improved.",
    "Standardize before you optimize.",
    "Be the designer of your world and not merely the consumer of it.",
    "Professionals stick to the schedule; amateurs let life get in the way.",
    "The most practical way to change who you are is to change what you do.",
    "Your identity emerges out of your habits.",
    "The seed of every habit is a single, tiny decision.",
    "Environment is the invisible hand that shapes human behavior.",
    "Habits do not restrict freedom. They create it.",
)

const val QUOTE_AUTHOR = "James Clear"

/** The quote for a local calendar day; rotates through [QUOTES] and is stable for the whole day. */
fun quoteFor(epochDay: Long): String = QUOTES[Math.floorMod(epochDay, QUOTES.size.toLong()).toInt()]
