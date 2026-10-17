package dna.familytree.feature.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlanMathTest {

    @Test
    fun trialPeriods() {
        assertEquals(PlanMath.Trial(7, false), PlanMath.trial("P7D"))
        assertEquals(PlanMath.Trial(14, false), PlanMath.trial("P2W"))
        assertEquals(PlanMath.Trial(1, true), PlanMath.trial("P1M"))
        assertEquals(PlanMath.Trial(12, true), PlanMath.trial("P1Y"))
        assertNull(PlanMath.trial(null))
        assertNull(PlanMath.trial("P0D"))
        assertNull(PlanMath.trial("garbage"))
    }

    @Test
    fun paysOff() {
        // 9.99 once vs 1.99 a month: 5.02 months, rounded up
        assertEquals(6, PlanMath.paysOffMonths(9_990_000, "USD", 1_990_000, "USD"))
        assertEquals(5, PlanMath.paysOffMonths(9_950_000, "USD", 1_990_000, "USD"))
        assertNull(PlanMath.paysOffMonths(9_990_000, "USD", 1_990_000, "EUR"))
        assertNull(PlanMath.paysOffMonths(1_000_000, "USD", 1_990_000, "USD")) // Cheaper than a month
        assertNull(PlanMath.paysOffMonths(99_990_000, "USD", 1_990_000, "USD")) // Over 3 years
        assertNull(PlanMath.paysOffMonths(9_990_000, "USD", 0, "USD"))
    }
}
