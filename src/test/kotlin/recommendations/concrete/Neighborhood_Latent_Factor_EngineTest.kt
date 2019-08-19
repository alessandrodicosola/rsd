package recommendations.concrete

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

internal class Neighborhood_Latent_Factor_EngineTest {
    @Test
    fun measureLoadingTimeInit() {
        assertTimeout(Duration.ofMinutes(1), { Neighborhood_Latent_Factor_Engine(20,20,0.04,0.04) })
    }
}