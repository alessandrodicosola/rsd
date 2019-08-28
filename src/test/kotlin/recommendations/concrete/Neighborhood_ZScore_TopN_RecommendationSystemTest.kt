package recommendations.concrete

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class Neighborhood_ZScore_TopN_RecommendationSystemTest {

    @Test
    fun test76561198015082830() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20, 5).getRecommendations(76561198015082830)
        }

    @Test
    fun test76561198014912110() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20, 5).getRecommendations(76561198015082830)
        }

    @Test
    fun test76561198009843480() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20, 5).getRecommendations(76561198009843480)
        }

    @Test
    fun test() {
        assertDoesNotThrow {
            val score = Neighborhood_ZScore_TopN_RecommendationSystem(20, 5).test()
            assertTrue(score.isFinite())
            print("rmse: $score")
        }
    }
}