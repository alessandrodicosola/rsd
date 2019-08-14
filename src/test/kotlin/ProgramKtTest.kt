import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem

internal class ProgramKtTest {

    @Test
    fun test76561198015082830() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198015082830)
        }

    @Test
    fun test76561198014912110() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198015082830)
        }

    @Test
    fun test76561198009843480() =
        assertDoesNotThrow {
            Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198009843480)
        }

}