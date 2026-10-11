package app.becoming.domain

object IdentityLimits {
    const val NAME_MAX = 30
    const val SKILL_MAX = 30
    const val TITLE_MAX = 32
}

enum class FieldError { EMPTY, TOO_LONG }

/** Cleaned values plus a per-field error (null = fine). */
data class IdentityValidation(
    val name: String,
    val skill: String,
    val identityTitle: String,
    val nameError: FieldError?,
    val skillError: FieldError?,
    val identityTitleError: FieldError?,
) {
    val isValid: Boolean get() = nameError == null && skillError == null && identityTitleError == null
}

/** Trims and collapses any run of inner whitespace (spaces, tabs, newlines) to a single space. */
fun normalizeIdentityField(raw: String): String {
    val out = StringBuilder()
    var pendingSpace = false
    for (ch in raw.trim()) {
        if (ch.isWhitespace()) {
            pendingSpace = true
        } else {
            if (pendingSpace && out.isNotEmpty()) out.append(' ')
            pendingSpace = false
            out.append(ch)
        }
    }
    return out.toString()
}

/** Validates the identity form: each field is normalised, must be non-empty and within its limit. */
fun validateIdentity(name: String, skill: String, identityTitle: String): IdentityValidation {
    val cleanName = normalizeIdentityField(name)
    val cleanSkill = normalizeIdentityField(skill)
    val cleanTitle = normalizeIdentityField(identityTitle)
    return IdentityValidation(
        name = cleanName,
        skill = cleanSkill,
        identityTitle = cleanTitle,
        nameError = fieldError(cleanName, IdentityLimits.NAME_MAX),
        skillError = fieldError(cleanSkill, IdentityLimits.SKILL_MAX),
        identityTitleError = fieldError(cleanTitle, IdentityLimits.TITLE_MAX),
    )
}

private fun fieldError(value: String, max: Int): FieldError? = when {
    value.isEmpty() -> FieldError.EMPTY
    value.length > max -> FieldError.TOO_LONG
    else -> null
}
