package recommendations.concrete

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.fillParameters
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import recommendations.skel.IRSEngine
import recommendations.skel.Neighbor
import recommendations.skel.RSObject
import recommendations.skel.User
import sql.dao.GamesDAO
import java.util.logging.Logger
import kotlin.system.measureTimeMillis
import kotlin.math.min

/**
 * Simple Neighborhood Recommendation System which retrieves recommendations with
 * - ZScore for calculating ratings
 * - TopN Neighbors for selecting neighbors
 */
class Neighborhood_ZScore_TopN_RecommendationSystem(val numberOfNeighbors: Int) :
    IRSEngine() {

    private var database: Database =
        Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")

    private val logger = Logger.getLogger(
        Neighborhood_ZScore_TopN_RecommendationSystem::javaClass.name
    )


    override fun getRecommendations(userId: Long): List<RSObject> {


        // 0.Generate user information
        logger.info("Retrieving user information")
        val user = getUserInformation(userId);

        // 1.Get neighbors
        logger.info("Retrieving neighbors for user: ${user.id}")
        var itemsAndNeighbors: Map<Int, List<Neighbor>> = mapOf()
        var elapsed = measureTimeMillis {
            itemsAndNeighbors = getItemsAndNeighbors(user)
        }
        logger.info("Time for computing neighbors: ${elapsed / 1000} s ")

        // 2.Calculate ratings for each item
        logger.info("Calculating ratings")
        var ratings: List<RSObject> = listOf()
        elapsed = measureTimeMillis {
            ratings = getRatings(user, itemsAndNeighbors)
        }
        logger.info("Time for computing ratings: ${elapsed / 1000} s ")

        // 3.Show List<RSObject>
        return ratings;
    }


    private fun getUserInformation(userId: Long): User {

        val user = User(userId, 0.0, 0.0)
        // Returns user's avg and std
        transaction {
            addLogger(StdOutSqlLogger)
            GamesDAO
                .slice(
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                )
                .select {
                    GamesDAO.SteamId eq userId
                }
                .groupBy(GamesDAO.SteamId)
                .map {
                    user.avg = it[GamesDAO.PlaytimeForever.avg()]?.toDouble() ?: 0.0
                    user.std = it[GamesDAO.PlaytimeForever.function("STD")]?.toDouble() ?: 0.0
                }
        }
        logger.info("User information: $user")
        return user
    }

    private fun getItemsAndNeighbors(user: User): Map<Int, List<Neighbor>> {

        /**
         * As suggested on page 57 line 12
         * Chapter2. A Comprehensive Survey of Neighborhood-Based Raccomandation Methods
         * @author Xia Ning, Christian Desrosiers and George Karypis
         */
        val factor = 25

        val userForMissingItems: MutableMap<Int, List<Long>> = mutableMapOf()

        transaction {
            addLogger(StdOutSqlLogger)

            // 1. Get all missing items for user and next the users who rated the item
            logger.info("Get all missing items for user ${user.id}")
            val statement =
                this.connection.prepareStatement("SELECT T1.appid FROM (SELECT DISTINCT appid FROM games_training ) AS T1 LEFT JOIN (SELECT * FROM games_training WHERE steamid = ? ) AS T2 ON T1.appid = T2.appid WHERE (playtime_forever = 0 OR playtime_forever IS NULL)")
            statement.setLong(1, user.id)
            val missingItems = statement.executeQuery().let {
                val out = arrayListOf<Int>()
                while (it.next()) {
                    out.add(it.getInt("T1.appid"))
                }
                out
            }
            logger.info("Get all users that rated missing items")
            missingItems.forEach {
                // 4. Get for each missing item users that rated it
                userForMissingItems[it] = GamesDAO.slice(GamesDAO.SteamId)
                    .select { GamesDAO.AppId eq it }
                    .map { it[GamesDAO.SteamId] }
            }
        }

        return userForMissingItems.mapValues { entry ->
            val neighborsCache: MutableMap<Long, Neighbor> = mutableMapOf()
            val neighborsId = entry.value

            var currentNeighbor = Neighbor(0, 0.0, 0.0, 0.0);

            for (id in neighborsId) {
                if (!neighborsCache.containsKey(id)) {

                    currentNeighbor.id = id

                    logger.info("Get neighbor [$id] information")

                    transaction {
                        addLogger(StdOutSqlLogger)

                        // 5.0 Calculate neighbor information
                        GamesDAO.slice(
                            GamesDAO.PlaytimeForever.avg(),
                            GamesDAO.PlaytimeForever.function("STD")
                        )
                            .select { GamesDAO.SteamId eq id }
                            .groupBy(GamesDAO.SteamId)
                            .map {
                                currentNeighbor.avg = it[GamesDAO.PlaytimeForever.avg()]?.toDouble() ?: 0.0
                                currentNeighbor.std = it[GamesDAO.PlaytimeForever.function("STD")]?.toDouble() ?: 0.0
                            }
                    }
                    neighborsCache[id] = currentNeighbor;
                } else currentNeighbor = neighborsCache[id]!!


                //Avoid neighbors with only one game and playtime_forever = 0 or NULL
                if (currentNeighbor.avg > 0.0 && currentNeighbor.std > 0) {

                    val itemsU: MutableMap<Int, Double> = mutableMapOf()
                    val itemsV: MutableMap<Int, Double> = mutableMapOf()


                    transaction {
                        addLogger(StdOutSqlLogger)
                        val t1 = GamesDAO.alias("T1")
                        val t2 = GamesDAO.alias("T2")
                        // 5.1 Calculate the set of items rated by U [User] and V [Neighbor]
                        t1.leftJoin(t2, { t1[GamesDAO.AppId] }, { t2[GamesDAO.AppId] })
                            .slice(t1[GamesDAO.AppId], t1[GamesDAO.PlaytimeForever], t2[GamesDAO.PlaytimeForever])
                            .select {
                                t1[GamesDAO.PlaytimeForever] greater 0 and (t2[GamesDAO.PlaytimeForever] greater 0)
                            }
                            .map {
                                itemsU.put(it[t1[GamesDAO.AppId]], it[t1[GamesDAO.PlaytimeForever]].toDouble())
                                itemsU.put(it[t1[GamesDAO.AppId]], it[t2[GamesDAO.PlaytimeForever]].toDouble())
                            }
                    }

                    currentNeighbor.weight =
                        WeightedPersonaCorrelation(
                            itemsU,
                            itemsV,
                            user,
                            currentNeighbor,
                            factor
                        ).calculate()

                    logger.info("Neighbor information: $currentNeighbor")

                    neighborsCache[id] = currentNeighbor
                }


            }

            neighborsCache.filter { it.value.weight > 0 }.map { it.value }
                .sortedByDescending { it.weight }
                .let {
                    return@mapValues it.slice(0..min(numberOfNeighbors, it.size - 1))
                }
        }

    }


    private fun getRatings(user: User, itemsAndNeighbors: Map<Int, List<Neighbor>>): List<RSObject> {
        val listOut: MutableList<RSObject> = mutableListOf()

        //1. Retrieve ratings given by V [Neighbor] for item i
        itemsAndNeighbors.filter { it.value.isNotEmpty() }.forEach { entry ->
            var ratings: Map<Long, Double> = mapOf()
            val neighbors = itemsAndNeighbors.get(entry.key)!!.map { it.id }

            transaction {
                ratings = GamesDAO.slice(GamesDAO.SteamId, GamesDAO.PlaytimeForever)
                    .select { GamesDAO.AppId eq entry.key and (GamesDAO.SteamId inList neighbors) }
                    .associate {
                        Pair(it[GamesDAO.SteamId], it[GamesDAO.PlaytimeForever].toDouble())
                    }
            }
            val hashRatings = HashMap(ratings)
            // 2. Calculate score
            val score = ZScoreRating(user, entry.value, hashRatings).calculate()
            listOut.add(RSObject(entry.key.toLong(), score))

        }

        return listOut
    }
}


