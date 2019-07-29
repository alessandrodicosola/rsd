import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem

internal class ProgramKtTest {

    @Test
    fun test76561198015082830() =
        assertTrue(Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198015082830).isNotEmpty())
    @Test
    fun test76561198014912110() =
        assertTrue(Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198014912110).isNotEmpty())


}