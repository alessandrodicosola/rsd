import com.beust.klaxon.json
import recommendations.concrete.CachedEngine
import recommendations.concrete.Neighborhood_Learning_Engine
import recommendations.concrete.Neighborhood_ZScore_TopN_RecommendationSystem
import recommendations.skel.IRSEngine
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.security.InvalidParameterException
import java.util.logging.Logger
import kotlin.math.pow

/* TODO
    - Koma - scientific library for pltting in kotlin
 */

fun main(args: Array<String>) {

    var type : Int = 0
    //edit conf for change engine
    if (!Files.exists(Path.of("conf"))) {
        File("conf").writer().write(0)
    }

    File("conf").useLines {
        type = it.elementAt(0).toInt()
    }

    val userId = args[1].toLong()
    val itemId = args[2].toInt()

    Logger.getGlobal().info("Calculating recommendations for user $userId -> $itemId...")

    var engine: IRSEngine<Long, Int, Double>? = null
    if (type == 0) {
        engine = Neighborhood_ZScore_TopN_RecommendationSystem(20, 6)
    } else if (type == 1) {
        engine = Neighborhood_Learning_Engine(20, 20, 0.00005, 0.0001, 10.0, 10.0.pow(-0.5))
    } else {
        throw InvalidParameterException("type")
    }

    val cachedEngine = CachedEngine(engine!!)

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