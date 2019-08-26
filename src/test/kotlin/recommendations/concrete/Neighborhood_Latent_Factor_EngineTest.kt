package recommendations.concrete

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.math.pow

internal class Neighborhood_Latent_Factor_EngineTest {

    @Test
    fun train() {
        assertDoesNotThrow {
            Neighborhood_Latent_Factor_Engine(20, 20, 0.0005, 0.00001, 1.0, 10.0.pow(-5)).train()
        }
    }
}