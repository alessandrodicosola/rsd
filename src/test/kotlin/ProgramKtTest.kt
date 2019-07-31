import logging.FileOutHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem
import java.nio.file.Paths
import java.util.logging.Logger

internal class ProgramKtTest {
    val current = Paths.get("").toAbsolutePath().resolve("log.txt")
    val logger = Logger.getGlobal().addHandler(FileOutHandler(current.toString()))

    @Test
    fun test76561198015082830() =
        assertTrue(Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198015082830).isNotEmpty())

    @Test
    fun test76561198014912110() =
        assertTrue(Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(76561198014912110).isNotEmpty())


}