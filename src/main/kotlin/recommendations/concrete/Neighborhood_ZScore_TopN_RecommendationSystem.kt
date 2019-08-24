package recommendations.concrete

import common.measureBlock
import common.toDoubleOrZero
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.*
import sql.dao.GamesDAO
import java.util.logging.Logger
import kotlin.math.min
import kotlin.to


/**
 * Simple Neighborhood Recommendation System which retrieves recommendations with
 * - ZScore for calculating ratings
 * - TopN Neighbors for selecting neighbors
 * - Persona Correlation weighted with a factor
 * - Each game is indexed with an Int
 */

//TODO Refactor this

class Neighborhood_ZScore_TopN_RecommendationSystem(
    private val numberOfNeighbors: Int,
    private val factorForNormalizeWeight: Int
) :
    IRSEngine<Long, Int, Double>(), ITestable<Double> {

    private var ratings: MutableMap<Long, MutableMap<Int, Double>>
    private var users: MutableMap<Long, User>
    private var items: MutableMap<Int, Item>

    init {
        var database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")


    }

    private fun initRatings() {
        transaction {
            measureBlock("Retrieve all ratings") {
                GamesDAO.slice(GamesDAO.SteamId, GamesDAO.AppId, GamesDAO.PlaytimeForever)
                    .select { GamesDAO.PlaytimeForever.isNotNull() and (GamesDAO.PlaytimeForever.greater(0)) };
            }.forEach {
                if (!ratings.containsKey(it[GamesDAO.SteamId])) ratings[it[GamesDAO.SteamId]] = mutableMapOf()

                ratings[it[GamesDAO.SteamId]]!![it[GamesDAO.AppId]] = it[GamesDAO.PlaytimeForever].toDoubleOrZero()
            }
        }
    }

    private fun initUsers() {
        transaction {

        }
    }

    override fun test(calculator: IErrorCalculator<Double>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    override fun getRecommendations(id: Long): List<RSObject<Int, Double>> {


        // 0.Generate user information
        logger.info("Retrieving user information")
        val user = measureBlock {
            getUserInformation(id)
        }

        // 1.Get neighbors
        logger.info("Retrieving neighbors for user: ${user.id}")
        val itemsAndNeighbors = measureBlock {
            getItemsAndNeighbors(user)
        }

        // 2.Calculate ratings for each item
        logger.info("Calculating ratings")
        val ratings = measureBlock {
            getRatings(user, itemsAndNeighbors)
        }

        // 3.Show List<RSObject>
        return ratings
    }

    private fun getUserInformation(userId: Long): User {

        val user = User(userId, 0.0, 0.0)
        // Returns user's avg and std
        transaction {

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
                    user.avg = it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero()
                    user.std = it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
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

        val userForMissingItems: Map<Int, List<Long>>

        val missingItems = transaction {
            // addLogger(StdOutSqlLogger)

            // 1. Get all missing items for user and next the users who rated the item
            logger.info("Get all missing items for user ${user.id}")

            /*
            val statement =
                this.connection.prepareStatement("SELECT T1.appid FROM (SELECT DISTINCT appid FROM games_training ) AS T1 LEFT JOIN (SELECT * FROM games_training WHERE steamid = ? ) AS T2 ON T1.appid = T2.appid WHERE (playtime_forever = 0 OR playtime_forever IS NULL)")
            statement.setLong(1, user.id)
            return@transaction statement.executeQuery().let {
                val out = arrayListOf<Int>()
                while (it.next()) {
                    out.add(it.getInt("T1.appid"))
                }
                out
            }
            */

            return@transaction measureBlock {
                val t1 = GamesDAO.slice(GamesDAO.AppId).selectAll().withDistinct().alias("T1")
                val t2 = GamesDAO.slice(GamesDAO.AppId, GamesDAO.PlaytimeForever).select { GamesDAO.SteamId eq user.id }
                    .alias("T2")
                t1.leftJoin(t2, { t1[GamesDAO.AppId] }, { t2[GamesDAO.AppId] })
                    .select { t2[GamesDAO.PlaytimeForever] eq 0 or t2[GamesDAO.PlaytimeForever].isNull() }
                    .map { it[t1[GamesDAO.AppId]] }
            }
        }

        logger.info("Get all users who rated missing items")
        userForMissingItems = measureBlock {
            transaction {
                return@transaction GamesDAO.slice(GamesDAO.SteamId, GamesDAO.AppId)
                    .select { GamesDAO.AppId inList missingItems }
                    .groupBy { it[GamesDAO.AppId] }.mapValues { it.value.map { it[GamesDAO.SteamId] } }
            }
        }

        return userForMissingItems.mapValues { entry ->
            val itemMissing = entry.key
            val neighborsCache: MutableMap<Long, Neighbor> = mutableMapOf()
            val neighborsId = entry.value

            var currentNeighbor = Neighbor(0, 0.0, 0.0, 0.0)

            for (currentNeighborId in neighborsId) {
                if (!neighborsCache.containsKey(currentNeighborId)) {

                    currentNeighbor.id = currentNeighborId

                    logger.info("Get neighbor [$currentNeighborId] information")

                    transaction {

                        // 5.0 Calculate neighbor information
                        GamesDAO.slice(
                            GamesDAO.PlaytimeForever.avg(),
                            GamesDAO.PlaytimeForever.function("STD")
                        )
                            .select { GamesDAO.SteamId eq currentNeighborId }
                            .groupBy(GamesDAO.SteamId)
                            .map {
                                currentNeighbor.avg = it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero()
                                currentNeighbor.std =
                                    it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
                            }
                    }
                    neighborsCache[currentNeighborId] = currentNeighbor
                } else currentNeighbor = neighborsCache[currentNeighborId]!!


                //Avoid neighbors with only one game or playtime_forever = 0 or NULL
                // if (currentNeighbor.std > 0) {

                val itemsU: MutableMap<Int, Double> = mutableMapOf()
                val itemsV: MutableMap<Int, Double> = mutableMapOf()


                transaction {

                    /* 5.1 Calculate the set of items rated by U [User] and V [Neighbor]
                        SELECT T1.appid,T1.playtime_forever,T2.playtime_forever FROM
                            (SELECT appid,playtime_forever FROM games_training WHERE steamid = ?) AS T1
                                LEFT JOIN
                            (SELECT appid,playtime_forever FROM games_training WHERE steamid = ?) AS T2
                                ON T1.appid = T2.appid
                        WHERE appid != itemMissing
                    */

                    val t1 = GamesDAO.slice(GamesDAO.AppId, GamesDAO.PlaytimeForever)
                        .select { GamesDAO.SteamId eq user.id }
                        .alias("T1")
                    val t2 = GamesDAO.slice(GamesDAO.AppId, GamesDAO.PlaytimeForever)
                        .select { GamesDAO.SteamId eq currentNeighborId }
                        .alias("T2")

                    t1.leftJoin(t2, { t1[GamesDAO.AppId] }, { t2[GamesDAO.AppId] })
                        .slice(t1[GamesDAO.AppId], t1[GamesDAO.PlaytimeForever], t2[GamesDAO.PlaytimeForever])
                        .select {
                            t1[GamesDAO.AppId] neq itemMissing
                        }.filter {
                            it.getOrNull(t1[GamesDAO.PlaytimeForever]) != null && it.getOrNull(t2[GamesDAO.PlaytimeForever]) != null
                        }
                        .map {
                            itemsU[it[t1[GamesDAO.AppId]]] = it[t1[GamesDAO.PlaytimeForever]].toDouble()
                            itemsV[it[t1[GamesDAO.AppId]]] = it[t2[GamesDAO.PlaytimeForever]].toDouble()
                        }
                }

                logger.info("${user.id} and $currentNeighborId have rated both ${itemsU.size} items ")

                currentNeighbor.weight = if (itemsU.isNotEmpty())
                    WeightedPersonaCorrelation(
                        itemsU,
                        itemsV,
                        user,
                        currentNeighbor,
                        factorForNormalizeWeight
                    ).calculate()
                else
                    0.0

                neighborsCache[currentNeighborId] = currentNeighbor

                logger.info("Neighbor information: $currentNeighbor")

                //}
            }

            neighborsCache.filter { it.value.weight > 0 }.map { it.value }
                .sortedByDescending { it.weight }
                .let {
                    return@mapValues it.slice(0..min(numberOfNeighbors - 1, it.size - 1))
                }
        }

    }


    private fun getRatings(user: User, itemsAndNeighbors: Map<Int, List<Neighbor>>): List<RSObject<Int, Double>> {
        val listOut: MutableList<RSObject<Int, Double>> = mutableListOf()

        //1. Retrieve ratings given by V [Neighbor] for item i
        itemsAndNeighbors.forEach { entry ->
            val itemId = entry.key
            val neighbors = entry.value.map { it.id }

            val ratings = transaction {
                //      addLogger(StdOutSqlLogger)
                return@transaction GamesDAO.slice(GamesDAO.SteamId, GamesDAO.PlaytimeForever)
                    .select { GamesDAO.AppId eq itemId and (GamesDAO.SteamId inList neighbors) and (GamesDAO.PlaytimeForever.isNotNull() or (GamesDAO.PlaytimeForever greater 0)) }
                    .associate {
                        it[GamesDAO.SteamId] to it[GamesDAO.PlaytimeForever].toDouble()
                    }
            }


            if (ratings.isNotEmpty()) {
                // 2. Calculate score
                val score = ZScoreRating(user, entry.value, ratings).calculate()
                listOut.add(itemId hasScore score)
            } else {
                listOut.add(itemId hasScore -1.0) //impossible to determine score
            }
        }
        return listOut
    }
}


