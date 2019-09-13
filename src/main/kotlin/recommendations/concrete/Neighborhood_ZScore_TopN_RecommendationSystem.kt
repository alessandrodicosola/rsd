package recommendations.concrete

import common.info
import common.measureBlock
import common.toDoubleOrZero
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.*
import sql.dao.GamesDAO
import sql.dao.GamesTestDAO
import kotlin.test.assertEquals


/**
 * Simple Neighborhood Recommendation System which retrieves recommendations with
 * - ZScore for calculating ratings
 * - TopN Neighbors for selecting neighbors
 * - Persona Correlation weighted with a factor
 * - Each game is indexed with an Int
 */


class Neighborhood_ZScore_TopN_RecommendationSystem(
    private val numberOfNeighbors: Int,
    private val factorForNormalizeWeight: Int
) :
    IRSEngine<Long, Int, Double>(), ITestable<Double> {



    override fun updateRecommendations(userId: Long, itemId: Int, ratingType: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


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


    override fun getRecommendations(userId: Long, itemId: Int): List<RSObject<Int, Double>>  {


        // 0.Get user information
        val user = users[userId]!!

        // 1.Get neighbors
        val itemsAndNeighbors = measureBlock("Getting neighbors for user: ${user.id}...") {
            getItemsAndNeighbors(user)
        }

        // 2.Calculate ratings for each item
        val ratings = measureBlock("Calculating ratings...") {
            getRatings(user, itemsAndNeighbors)
        }

        // 3.Show List<RSObject>
        return ratings
    }

    private fun getNeighborsForItem(
        user: User,
        itemId: Int,
        ratingsUser: MutableMap<Int, Double>,
        userItems: List<Int>
    ): List<Neighbor> {

        val outmap = mutableListOf<Neighbor>()

        for (entry in ratings.filter { it.value.containsKey(itemId) }) {
            val neighborId = entry.key

            val ratingsNeighbor = entry.value
            val itemsNeighbor = ratingsNeighbor.map { it.key }

            val itemsRatedByBoth = userItems.intersect(itemsNeighbor)

            if (itemsRatedByBoth.isNotEmpty()) {
                val currentNeighbor = Neighbor(users[neighborId]!!, 0.0)
                val itemsU = ratingsUser.filter { itemsRatedByBoth.contains(it.key) }
                val itemsV = entry.value.filter { itemsRatedByBoth.contains(it.key) }
                info("${user.id} and ${currentNeighbor.id} have rated both ${itemsU.size} items ")
                currentNeighbor.weight = WeightedPersonaCorrelation(
                    itemsU,
                    itemsV,
                    user,
                    currentNeighbor,
                    factorForNormalizeWeight
                ).calculate()

                outmap.add(currentNeighbor)

            }
        }
        return outmap.asSequence().filter { it.weight > 0 }.sortedByDescending { it.weight }.take(numberOfNeighbors)
            .toList()
    }

    private fun getItemsAndNeighbors(user: User): Map<Int, List<Neighbor>> {


        val ratingsUser = ratings[user.id]!!
        val userItems = ratingsUser.asSequence().map { it.key }
        val allItems = items.asSequence().map { it.key }
        val missingItems = allItems - userItems

        val outMap = mutableMapOf<Int, List<Neighbor>>()

        missingItems.forEach {
            outMap[it] = getNeighborsForItem(user, it, ratingsUser, userItems.toList())
        }

        return outMap
    }


    private fun getRatings(
        user: User,
        itemsAndNeighbors: Map<Int, List<Neighbor>>
    ): List<RSObject<Int, Double>> {
        val listOut: MutableList<RSObject<Int, Double>> = mutableListOf()

        //1. Retrieve ratings given by V [Neighbor] for item i
        itemsAndNeighbors.forEach { entry ->
            val itemId = entry.key

            val score = getRate(user, itemId, entry.value)

            listOut.add(itemId hasScore score)
        }

        return listOut
    }

    private fun getRate(user: User, itemId: Int, neighbors: List<Neighbor>): Double {
        val neighborsRating = neighbors.asSequence().associate {
            val map = ratings[it.id]!!
            val rate = map[itemId]!!
            it.id to rate
        }
        return ZScoreRating(user, neighbors, neighborsRating).calculate()
    }

    override fun test(): Double {

        val excludedItems = mutableListOf<Pair<Long, Int>>()
        val excludedUsers = mutableListOf<Long>()


        var testRatings = mutableMapOf<Long, MutableMap<Int, Double>>()
        transaction {
            measureBlock("Retrieve all test ratings") {
                GamesTestDAO.slice(GamesTestDAO.SteamId, GamesTestDAO.AppId, GamesTestDAO.PlaytimeForever)
                    .select { GamesTestDAO.PlaytimeForever.isNotNull() }
            }.forEach {

                testRatings.putIfAbsent(it[GamesTestDAO.SteamId], mutableMapOf())

                testRatings[it[GamesTestDAO.SteamId]]!!.putIfAbsent(
                    it[GamesTestDAO.AppId],
                    it[GamesTestDAO.PlaytimeForever].toDoubleOrZero()
                )

            }
        }
        val maxUser = 30
        val selectedUsers = (1..maxUser).map { testRatings.keys.random() }
        testRatings = testRatings.filterKeys { selectedUsers.contains(it) }.toMutableMap()

        assertEquals(selectedUsers.size, testRatings.size)

        val calculatedRatings = mutableMapOf<Long, MutableMap<Int, Double>>()

        for (user in testRatings) {
            val selectedUser = users[user.key]!!

            if (ratings[selectedUser.id] == null) {
                excludedUsers.add(selectedUser.id)
                continue
            }

            for (item in user.value) {
                val userRatings = ratings[selectedUser.id]!!
                val userItems = userRatings.asSequence().map { it.key }.toList()
                val neighbors = getNeighborsForItem(selectedUser, item.key, userRatings, userItems)
                val prediction = getRate(selectedUser, item.key, neighbors)

                if (!prediction.isFinite()) {
                    //probably not neighbors present for the item present in GamesTest so prediction is nan
                    excludedItems.add(selectedUser.id to item.key)
                    continue
                }

                calculatedRatings.putIfAbsent(selectedUser.id, mutableMapOf())
                calculatedRatings[selectedUser.id]!![item.key] = prediction
            }
        }


        excludedUsers.forEach { testRatings.remove(it) }
        testRatings.forEach { entry ->
            excludedItems.forEach {
                if (entry.key == it.first) entry.value.remove(it.second)
            }
        }


        val trueRatings = testRatings.asSequence().flatMap { it.value.values.asSequence() }.toList().toDoubleArray()
        val predictedRatings =
            calculatedRatings.asSequence().flatMap { it.value.values.asSequence() }.toList().toDoubleArray()
        return RMSECalculator(trueRatings, predictedRatings).calculate()
    }


}


