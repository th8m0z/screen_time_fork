package com.solusibejo.screen_time.util

import java.util.Locale

object EnumExtension {
    fun String.toEnumFormat(): String {
        return replace(Regex("([a-z])([A-Z])"), "$1_$2") // Insert underscores
            .uppercase(Locale.getDefault()) // Convert to uppercase
    }

    fun String.toCamelCase(): String {
        return lowercase()
            .split("_") // Split by underscore
            .mapIndexed { index, word -> if (index == 0) word else word.replaceFirstChar { it.uppercase() } } // Capitalize words except first
            .joinToString("") // Join back
    }
}