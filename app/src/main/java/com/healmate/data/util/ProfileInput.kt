package com.healmate.data.util

private val isoDate = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

internal fun isBirthDateFormat(value: String): Boolean = isoDate.matches(value.trim())
