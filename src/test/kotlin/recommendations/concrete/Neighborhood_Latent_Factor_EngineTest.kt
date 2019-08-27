package recommendations.concrete

import common.info
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.logging.Logger
import kotlin.math.pow

internal class Neighborhood_Latent_Factor_EngineTest {

    @Test
    fun train() {
        assertDoesNotThrow {
            Neighborhood_Latent_Factor_Engine(
                20,
                20,
                0.0005,
                0.00001,
                1.0,
                10.0.pow(-5)
            ) //train it's called in the constructor
        }
    }

    @Test
    fun test() {
        var result = 0.0
        assertDoesNotThrow {
            result = Neighborhood_Latent_Factor_Engine(20, 20, 0.0005, 0.00001, 1.0, 10.0.pow(-5)).test()
            assert(result.isFinite())

        }
        print("rmse: $result")
    }
}