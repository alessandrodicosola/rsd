package common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class UtilKtTest {

    @Test
    fun toDoubleOrZero() {
        val intnonnull: Int = 1;
        val doublenonnull: Double = 1.3213;

        val intNull: Int? = null;
        val int_NullbutNonNull: Int? = 212143
        val double_NullbutNonNull: Double? = 213.432

        assertEquals(1.0, intnonnull.toDoubleOrZero())
        assertEquals(1.3213, doublenonnull.toDoubleOrZero())
        assertEquals(0.0, intNull.toDoubleOrZero())
        assertEquals(212143.0, int_NullbutNonNull.toDoubleOrZero())
        assertEquals(213.432, double_NullbutNonNull.toDoubleOrZero())

    }
}