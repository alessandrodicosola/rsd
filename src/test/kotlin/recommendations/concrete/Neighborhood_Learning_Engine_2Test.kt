package recommendations.concrete

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.math.pow

internal class Neighborhood_Learning_Engine_2Test {

    @Test
    fun train() {
        assertDoesNotThrow {
            Neighborhood_Learning_Engine_2(20, 20, 0.00005,0.001, 1.0, 10.0.pow(-5)).train()
        }
    }

    @Test
    fun test() {
    }
}