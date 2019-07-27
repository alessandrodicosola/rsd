

import com.beust.klaxon.json
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem
import java.util.logging.Logger

fun main(args: Array<String>) {
    val userId = args[0].toLong()
    Logger.getGlobal().info("Calculating recommendations for user $userId ...")
    val list = Neighborhood_ZScore_TopN_RecommendationSystem(20).getRecommendations(userId)

    print(
        json {
            list.map {
                obj(it.id.toString() to it.score.toString())
            }
        }
    )

}