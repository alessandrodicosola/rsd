import com.beust.klaxon.json

import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem
import recommendations.skel.RSObject
import java.nio.file.Paths
import java.util.logging.Logger

fun main(args: Array<String>) {


    for (userId in args) {
        Logger.getGlobal().info("Calculating recommendations for user $userId ...")
        val list = Neighborhood_ZScore_TopN_RecommendationSystem(40).getRecommendations(userId.toLong())

        if (list.isEmpty()) print(json {
            obj(
                "error" to false,
                "message" to "No recommendations found for user: $userId"
            )
        }.toJsonString())
        else {
            print(
                json {
                    array(
                        list.map { obj(it.id.toString() to it.score.toString()) }
                    )
                }.toJsonString()
            )
        }
    }

}