import com.beust.klaxon.json
import recommendations.concrete.CachedEngine
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem
import java.util.logging.Logger


fun main(args: Array<String>) {

    val userId = args[0].toLong()

    Logger.getGlobal().info("Calculating recommendations for user $userId ...")
    val engine = Neighborhood_ZScore_TopN_RecommendationSystem(40, 5)
    val cachedEngine = CachedEngine(engine)

    val MAX = 20

    val list =
        cachedEngine.getRecommendations(userId)

    if (list.isEmpty()) print(json {
        obj(
            "error" to false,
            "message" to "No recommendations found for user: $userId",
            "recommendations" to ""
        )
    }.toJsonString())
    else {
        print(
            json {
                obj(
                    "error" to false,
                    "message" to "",
                    "recommendations" to array(list.map { obj("id" to it.id, "score" to it.score) })
                )
            }.toJsonString()
        )
    }


}