package com.example.interest_calculator.ui

/**
 * Indian digit grouping (1,00,000 / 10,00,000), done by hand so it is identical on every
 * device and in JVM tests (java.text.DecimalFormat cannot express lakh/crore grouping).
 */
fun groupIndian(value: Long): String {
    val raw = value.toString()
    val negative = raw.startsWith('-')
    val digits = raw.removePrefix("-")
    if (digits.length <= 3) return raw
    val head = digits.dropLast(3)
    val tail = digits.takeLast(3)
    val groups = ArrayDeque<String>()
    var rest = head
    while (rest.length > 2) {
        groups.addFirst(rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) groups.addFirst(rest)
    return (if (negative) "-" else "") + groups.joinToString(",") + "," + tail
}

/** "₹1,00,000" */
fun formatRupees(amount: Int): String = "₹" + groupIndian(amount.toLong())

/** "₹1,15,000.00" from a value in paise (v1's rounded total × 100). */
fun formatRupeesFromCents(cents: Long): String {
    val negative = cents < 0
    val whole = groupIndian(cents / 100).removePrefix("-")
    val fraction = (cents % 100).toString().removePrefix("-").padStart(2, '0')
    return (if (negative) "-₹" else "₹") + whole + "." + fraction
}

/** "1.25" */
fun formatRate(hundredths: Int): String =
    "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"

/** "2 yrs 3 mos 15 days", matching the 30-day-month period v1 used. */
fun formatPeriod(years: Int, months: Int, days: Int): String {
    fun part(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"
    val parts = buildList {
        if (years != 0) add(part(years, "yr", "yrs"))
        if (months != 0) add(part(months, "mo", "mos"))
        if (days != 0 || isEmpty()) add(part(days, "day", "days"))
    }
    return parts.joinToString(" ")
}
