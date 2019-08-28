package recommendations.concrete

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import kotlin.math.pow

internal class Neighborhood_Latent_Factor_EngineTest {

    @Test
    fun train() {
        assertDoesNotThrow {
            Neighborhood_Learning_Engine(
                20,
                20,
                0.00005,
                0.00001,
                1.0,
                10.0.pow(-5)
            ).train()
        }
    }

    @Test
    fun test() {
        var result = 0.0
        assertDoesNotThrow {
            result = Neighborhood_Learning_Engine(20, 20, 0.00005, 0.00001, 1.0, 10.0.pow(-5)).test()
            assert(result.isFinite())

        }
        print("rmse: $result")
    }
}