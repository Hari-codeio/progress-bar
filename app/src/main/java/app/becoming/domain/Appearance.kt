package app.becoming.domain

/**
 * Effective dark mode. [setting] is the user's stored choice: null follows the system,
 * true/false forces dark/light.
 */
fun resolveDark(setting: Boolean?, systemDark: Boolean): Boolean = setting ?: systemDark
