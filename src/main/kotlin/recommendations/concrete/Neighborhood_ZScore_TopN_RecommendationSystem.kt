package recommendations.concrete

import common.info
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

    private var ratings: MutableMap<Long, MutableMap<Int, Double>> = mutableMapOf()
    private var users: MutableMap<Long, User> = mutableMapOf()
    private var items: MutableMap<Int, Item> = mutableMapOf()

    init {
        var database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")

        initRatings()
        initDataStructures()
    }

    /**
     * Init ratings information
     */
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

    /**
     * Init users and ites information
     */
    private fun initDataStructures() {
        users = transaction {
            measureBlock("Retrieve all users information") {
                GamesDAO.slice(
                    GamesDAO.SteamId,
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                ).selectAll().groupBy(GamesDAO.SteamId)
            }.associate {
                it[GamesDAO.SteamId] to
                        User(
                            it[GamesDAO.SteamId],
                            it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero(),
                            it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
                        )
            }.toMutableMap()
        }

        items = transaction {
            measureBlock("Retrieve all items information") {
                GamesDAO.slice(
                    GamesDAO.AppId,
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                )
                    .selectAll().groupBy(GamesDAO.AppId).associate {
                        it[GamesDAO.AppId] to
                                Item(
                                    it[GamesDAO.AppId],
                                    it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero(),
                                    it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
                                )
                    }.toMutableMap()
            }
        }
    }


    override fun getRecommendations(id: Long): List<RSObject<Int, Double>> {


        // 0.Get user information
        val user = users[id]!!

        // 1.Get neighbors
        val itemsAndNeighbors = measureBlock("Get neighbors for user: ${user.id}") {
            getItemsAndNeighbors(user)
        }

        // 2.Calculate ratings for each item
        val ratings = measureBlock {
            getRatings(user, itemsAndNeighbors)
        }

        // 3.Show List<RSObject>
        return ratings
    }


    private fun getItemsAndNeighbors(user: User): Map<Int, List<Neighbor>> {

        val neighborsCache: MutableList<Neighbor> = mutableListOf()

        val allItems = items.asSequence().map { it.key }
        val userItems = ratings[user.id]!!.asSequence().map { it.key }
        val missingItems = allItems - userItems

        val itemsU = ratings[user.id]!!

        val userForMissingItems: MutableMap<Int, MutableList<Long>> = mutableMapOf()

        measureBlock("Get users that rated missing items from ${user.id} ratings") {
            ratings.forEach { entry ->
                missingItems.forEach {
                    if (entry.value.containsKey(it)) userForMissingItems[it]!!.add(entry.key)
                }
            }
        }

        return userForMissingItems.mapValues { entry ->
            val neighbors = entry.value

            for (currentNeighborId in neighbors) {

                //Avoid neighbors with only one game or playtime_forever = 0 or NULL
                // if (currentNeighbor.std > 0) {
                val currentNeighbor = Neighbor(users[currentNeighborId]!!, 0.0)

                val itemsNeighbor = ratings[currentNeighborId]!!
                // items rated by neighbor and also user
                val itemsV = itemsNeighbor.filter { userItems.contains(it.key) }

                info("${user.id} and $currentNeighborId have rated both ${itemsU.size} items ")

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

                neighborsCache.add(currentNeighbor)
            }

            neighborsCache.asSequence().filter { it.weight > 0 }.map { it }
                .sortedByDescending { it.weight }
                .let {
                    return@mapValues it.take(numberOfNeighbors).toList()
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

    override fun test(calculator: IErrorCalculator<Double>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}


