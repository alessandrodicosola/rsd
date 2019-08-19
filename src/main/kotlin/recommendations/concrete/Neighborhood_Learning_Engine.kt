package recommendations.concrete

import logging.measureBlock
import logging.toDoubleOrZero
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.IRSEngine
import recommendations.skel.Item
import recommendations.skel.RSObject
import recommendations.skel.User
import sql.dao.GamesDAO
import java.lang.Math.pow
import java.lang.Thread.yield
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.streams.asStream

/**
 * Ratings are calculated with a trained recommendation system
 * @param factors Max factors to extract
 * @param iterations Max iterations allowed
 * @param learningRate
 * @param lambda1
 */
class Neighborhood_Latent_Factor_Engine(
    private val iterations: Int,
    private val factors: Int,
    private val learningRate: Double,
    private val lambda1: Double
) : IRSEngine<Long>() {

    /// Map<SteamId,List<(AppId,Rating)>
    private var ratings = mutableMapOf<Long, MutableList<Pair<Int, Double>>>()
    private var users = listOf<User>()
    private var items = listOf<Item>()

    private var latentUsers: MutableMap<Long, MutableList<Double>>
    private var latentItems: MutableMap<Int, MutableList<Double>>
    /// Map<Long,List<AppId,List<Factor>>>
    private var latentImplicit: MutableMap<Long, MutableList<Pair<Int, MutableList<Double>>>>

    init {
        // Load in memory all ratings,users and items
        val database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")

        transaction {
            measureBlock("Retrieve all ratings") {
                GamesDAO.slice(GamesDAO.SteamId, GamesDAO.AppId, GamesDAO.PlaytimeForever).selectAll();
            }.forEach {
                if (!ratings.containsKey(it[GamesDAO.SteamId])) ratings[it[GamesDAO.SteamId]] = mutableListOf()

                ratings[it[GamesDAO.SteamId]]!!.add(it[GamesDAO.AppId] to it[GamesDAO.PlaytimeForever].toDoubleOrZero())
            }
        }
        users = transaction {
            measureBlock("Retrieve all users information") {
                GamesDAO.slice(
                    GamesDAO.SteamId,
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                ).selectAll().groupBy(GamesDAO.SteamId)
            }.map {
                User(
                    it[GamesDAO.SteamId],
                    it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero(),
                    it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
                )
            }
        }
        items = transaction {
            measureBlock("Retrieve all items information") {
                GamesDAO.slice(GamesDAO.AppId, GamesDAO.PlaytimeForever.avg(), GamesDAO.PlaytimeForever.function("STD"))
                    .selectAll().groupBy(GamesDAO.AppId).map {
                        Item(
                            it[GamesDAO.AppId],
                            it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero(),
                            it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrZero()
                        )
                    }
            }
        }


        val random = Random(System.currentTimeMillis())

        // Initialize factors of users
        var halt = factors
        latentUsers = measureBlock("Initialize random latent users factors") {
            users.associate {
                it.id to generateSequence {
                    if (halt > 0) {
                        halt--
                        random.nextDouble()
                    } else null
                }.toMutableList()
            }.toMutableMap()
        }

        //Initialize factors of items
        halt = factors;
        latentItems = measureBlock("Initialize random latent items factors") {

            items.associate {
                it.id to generateSequence {
                    if (halt > 0) {
                        halt--
                        random.nextDouble()
                    } else null
                }.toMutableList()
            }.toMutableMap()
        }

        // Initialize factors of implicits
        halt = factors;
        latentImplicit = measureBlock("Initialize random latent implicit factors") {
            ratings.mapValues {
                it.value.map {
                    it.first to generateSequence {
                        if (halt > 0) {
                            halt--
                            random.nextDouble()
                        } else null
                    }.toMutableList()
                }.toMutableList()
            }.toMutableMap()
        }
    }

    private fun train() {
        for (iteration in 1..iterations) {

            for (factor in 1..factors) {

                for (user in users) {

                    for (item in items) {
                        //TODO("train")
                    }
                }
            }

        }
    }


    /**
     * @param user
     * @param item
     * @param error
     * @param learningRate
     * @param lambda1
     * @param latentUsers Map<SteamId,List<Factor>> where latent factors are contained
     * @param latentItems Map<AppId,List<Factor>> where latent factors are contained
     * @param latentImplicit Map<AppId,List<Factor>> where implicit factor about implicit feedback are contained
     */
    private fun trainUserItem(
        user: User,
        item: Item,
        error: Double,
        currentFactor: Int,
        latentUsers: MutableMap<Long, MutableList<Double>>,
        latentItems: MutableMap<Int, MutableList<Double>>,
        latentImplicit: MutableMap<Int, MutableList<Double>>
    ) {
        // Get global information about dataset
        var biasUser = user.avg;
        var biasItem = item.avg;

        // b_u
        biasUser = biasUser + learningRate * error - lambda1 * biasUser;
        user.avg = biasUser;

        // b_i
        biasItem = biasItem + learningRate * error - lambda1 * biasItem;
        item.avg = biasItem;

        // Get user's interaction with items
        // Get user characterization relative his factors and his implicit feedback
        val factorWichNormalizeSumImplicit = latentImplicit.count().toDouble().pow(-0.5);
        val implicitSumRelativeCurrentFactor = latentImplicit.map { it.value.get(currentFactor) }.sumByDouble { it }
        val userInteraction =
            latentUsers[user.id]!![currentFactor] + factorWichNormalizeSumImplicit * implicitSumRelativeCurrentFactor

        // q_if
        latentItems[item.id]!![currentFactor] =
            latentItems[item.id]!![currentFactor] + (learningRate * error * userInteraction) - lambda1 * latentItems[item.id]!![currentFactor]
        // p_if
        latentUsers[user.id]!![currentFactor] =
            latentUsers[user.id]!![currentFactor] + learningRate * error * latentItems[item.id]!![currentFactor]

        val updatedImplicit = mapOf<Int, MutableList<Double>>();

        for (implicit in latentImplicit) {
            updatedImplicit[implicit.key]!![currentFactor] =
                implicit.value[currentFactor] + (error * latentItems[item.id]!![currentFactor] * factorWichNormalizeSumImplicit) - lambda1 * implicit.value[currentFactor];
        }
        updatedImplicit.forEach { latentImplicit[it.key] = it.value }
    }

    override fun getRecommendations(id: Long): List<RSObject<Long>> {
        TODO("Not implemented")
    }


}