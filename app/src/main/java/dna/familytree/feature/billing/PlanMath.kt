package dna.familytree.feature.billing

/**
 * Price maths of the paywall, pure for unit tests (PlanMathTest).
 */
object PlanMath {

    /** A free trial: [count] days or months. */
    data class Trial(val count: Int, val months: Boolean)

    /** Reads an ISO-8601 billing period of Google Play ("P7D", "P1W", "P1M", "P3M"), weeks become days. */
    fun trial(period: String?): Trial? {
        val match = Regex("""P(\d+)([DWMY])""").matchEntire(period ?: return null) ?: return null
        val count = match.groupValues[1].toInt()
        if (count <= 0) return null
        return when (match.groupValues[2]) {
            "D" -> Trial(count, false)
            "W" -> Trial(count * 7, false)
            "M" -> Trial(count, true)
            else -> Trial(count * 12, true)
        }
    }

    /**
     * How many months of the monthly plan cost as much as the lifetime purchase, or null when it is not a selling
     * point (different currencies, missing prices, or more than [maxMonths]).
     */
    fun paysOffMonths(lifetimeMicros: Long, lifetimeCurrency: String, monthlyMicros: Long, monthlyCurrency: String,
                      maxMonths: Int = 36): Int? {
        if (lifetimeMicros <= 0 || monthlyMicros <= 0 || lifetimeCurrency != monthlyCurrency) return null
        val months = ((lifetimeMicros + monthlyMicros - 1) / monthlyMicros).toInt()
        return if (months in 2..maxMonths) months else null
    }
}
