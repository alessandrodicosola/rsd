package recommendations.concrete

import recommendations.skel.IRSEngine
import recommendations.skel.IRatingCalculator
import recommendations.skel.RSObject

class NeighborhoodRecommendationSystem(ratingCalculator: IRatingCalculator) : IRSEngine(ratingCalculator) {
    override fun getRecommendations(userId: Long): List<RSObject> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}