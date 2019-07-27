package recommendations.concrete

import org.jetbrains.exposed.sql.avg
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.stdDevPop
import org.jetbrains.exposed.sql.stdDevSamp
import recommendations.skel.IRSEngine
import recommendations.skel.IRatingCalculator
import recommendations.skel.RSObject
import sql.dao.GamesTrainingDAO

class NeighborhoodRecommendationSystem(ratingCalculator: IRatingCalculator) : IRSEngine(ratingCalculator) {
    override fun getRecommendations(userId: Long): List<RSObject> {
        // 0.Generate user information
        GamesTrainingDAO
            .slice(GamesTrainingDAO.SteamId,GamesTrainingDAO.PlaytimeForever.avg(),GamesTrainingDAO.PlaytimeForever.stdDevSamp())
            .select(GamesTrainingDAO.SteamId eq userId )
            .groupBy(GamesTrainingDAO.PlaytimeForever)

        // 1.Get neighbors


        // 2.Calculate ratings
        // 3.Show List<RSObject>
        TODO("implement me")
    }
}