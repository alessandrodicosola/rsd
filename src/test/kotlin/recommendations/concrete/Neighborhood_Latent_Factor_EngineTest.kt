package recommendations.concrete

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

internal class Neighborhood_Latent_Factor_EngineTest {

    @Test
    fun train() {
        assertDoesNotThrow {
            Neighborhood_Latent_Factor_Engine(10, 10, 0.000005,0.0000001, 1.0).Train()
        }
    }
}