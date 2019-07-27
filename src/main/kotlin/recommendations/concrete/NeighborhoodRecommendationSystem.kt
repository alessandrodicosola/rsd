package recommendations.concrete

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.IRSEngine
import recommendations.skel.Neighbor
import recommendations.skel.RSObject
import recommendations.skel.User
import sql.dao.GamesTrainingDAO
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

/**
 * Simple Neighborhood Recommendation System which retrieves recommendations with
 * - ZScore for calculating ratings
 * - TopN Neighbors for selecting neighbors
 */
class Neighborhood_ZScore_TopN_RecommendationSystem(val numberOfNeighbors: Int) :
    IRSEngine() {

    private var database: Database =
        Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")

    private val logger = Logger.getLogger(Neighborhood_ZScore_TopN_RecommendationSystem::javaClass.name)

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
            GamesTrainingDAO
                .slice(
                    GamesTrainingDAO.PlaytimeForever.avg(),
                    GamesTrainingDAO.PlaytimeForever.function("STD")
                )
                .select {
                    GamesTrainingDAO.SteamId eq userId
                }
                .groupBy(GamesTrainingDAO.SteamId)
                .map {
                    user.avg = it[GamesTrainingDAO.PlaytimeForever.avg()]!!.toDouble()
                    user.std = it[GamesTrainingDAO.PlaytimeForever.function("STD")]!!.toDouble()
                }
        }
        return user
    }

    private fun getItemsAndNeighbors(user: User): Map<Int, List<Neighbor>> {

        /**
         * As suggested on page 57 line 12
         * Chapter2. A Comprehensive Survey of Neighborhood-Based Raccomandation Methods
         * @author Xia Ning, Christian Desrosiers and George Karypis
         */
        val factor = 25
        var missingItems: List<Int>
        val userForMissingItems: MutableMap<Int, List<Long>> = mutableMapOf()

        transaction {
            addLogger(StdOutSqlLogger)

            // 1. Get all items in database
            val allItems = GamesTrainingDAO.slice(GamesTrainingDAO.AppId).selectAll().withDistinct()
                .map { it[GamesTrainingDAO.AppId] }

            // 2. Get all user items
            val userItems = GamesTrainingDAO.slice(GamesTrainingDAO.AppId)
                .select { GamesTrainingDAO.SteamId eq user.id }
                .withDistinct().map { it[GamesTrainingDAO.AppId] }

            // 3. Calculate missing items
            missingItems = allItems - userItems

            missingItems.forEach {
                // 4. Get for each item users that rated it
                userForMissingItems.put(it,
                    GamesTrainingDAO.slice(GamesTrainingDAO.SteamId).select { GamesTrainingDAO.AppId eq it }
                        .map { it[GamesTrainingDAO.SteamId] })

            }


        }

        return userForMissingItems.mapValues {
            val neighbors: MutableList<Neighbor> = mutableListOf()
            val neighborsId = it.value

            for (id in neighborsId) {

                val neighbor = Neighbor(id, 0.0, 0.0, 0.0)

                val itemsU: HashMap<Long, Double> = hashMapOf()
                val itemsV: HashMap<Long, Double> = hashMapOf()

                transaction {
                    addLogger(StdOutSqlLogger)

                    // 5.0 Calculate neighbor information
                    GamesTrainingDAO.slice(
                        GamesTrainingDAO.PlaytimeForever.avg(),
                        GamesTrainingDAO.PlaytimeForever.function("STD")
                    )
                        .select { GamesTrainingDAO.SteamId eq id }
                        .groupBy(GamesTrainingDAO.SteamId)
                        .map {
                            neighbor.avg = it[GamesTrainingDAO.PlaytimeForever.avg()]!!.toDouble()
                            neighbor.std = it[GamesTrainingDAO.PlaytimeForever.function("STD")]!!.toDouble()
                        }

                    //Avoid neighbors with only one game and playtime_forever = 0
                    if (neighbor.avg > 0.0) {

                            TODO("itemsU and itemsV contains different keys")

                        // 5.1 Calculate the set of items rated by U [User] and V [Neighbor]
                        GamesTrainingDAO
                            .select { (GamesTrainingDAO.SteamId inList listOf(user.id, id)) }
                            .map {
                                if (it[GamesTrainingDAO.SteamId] == user.id) {
                                    itemsU.put(
                                        it[GamesTrainingDAO.AppId].toLong(),
                                        it[GamesTrainingDAO.PlaytimeForever].toDouble()

                                    )
                                } else {
                                    itemsV.put(
                                        it[GamesTrainingDAO.AppId].toLong(),
                                        it[GamesTrainingDAO.PlaytimeForever].toDouble()

                                    )
                                }
                            }
                    }

                    WeightedPersonaCorrelation(itemsU, itemsV, user, neighbor, factor).calculate()
                    neighbors.add(neighbor)
                }
            }
            neighbors.sortByDescending { it.weight }
            return@mapValues neighbors.slice(0..numberOfNeighbors)
        }
    }

    private fun getRatings(user: User, itemsAndNeighbors: Map<Int, List<Neighbor>>): List<RSObject> {
        val listOut: MutableList<RSObject> = mutableListOf()

        //1. Retrieve ratings given by V [Neighbor] for item i
        itemsAndNeighbors.forEach { entry ->
            var ratings: Map<Long, Double> = mapOf()
            val neighbors = itemsAndNeighbors.get(entry.key)!!.map { it.id }
            transaction {
                ratings = GamesTrainingDAO.slice(GamesTrainingDAO.PlaytimeForever)
                    .select { GamesTrainingDAO.AppId eq entry.key and (GamesTrainingDAO.SteamId inList neighbors) }
                    .associate {
                        Pair(it[GamesTrainingDAO.SteamId], it[GamesTrainingDAO.PlaytimeForever].toDouble())
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
