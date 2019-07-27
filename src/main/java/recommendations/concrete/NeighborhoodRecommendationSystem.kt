package recommendations.concrete

import recommendations.skel.IRSEngine
import recommendations.skel.IRatingCalculator
import recommendations.skel.RSObject

class NeighborhoodRecommendationSystem(ratingCalculator: IRatingCalculator) : IRSEngine(ratingCalculator) {
    override fun getRecommendations(userId: Long): List<RSObject> {

        // 1.Get neighbors
        // 2.Calculate ratings
        // 3.Show List<RSObject>
        TODO("implement me")
    }
}