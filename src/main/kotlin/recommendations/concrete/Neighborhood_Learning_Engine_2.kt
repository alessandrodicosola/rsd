@file:Suppress("DuplicatedCode")

package recommendations.concrete

import common.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.*
import sql.dao.GamesDAO
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.assertTrue

/**
 *  Ratings are calculated with a trained recommender system
 */
@Deprecated("Diverge")
class Neighborhood_Learning_Engine_2(
    private val iterations: Int,
    private val factors: Int,
    private val learningRate: Double,
    private val decreaseLearningRateOf: Double,
    private val lambda1: Double,
    private val errorTollerance: Double
) : IRSEngine<Long, Int, Double>(), ITrainable, ITestable<Double> {
    override fun getRecommendations(userId: Long, itemId: Int): List<RSObject<Int, Double>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateRecommendations(userId: Long, itemId: Int, ratingType: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    /* init .cache dir */
    private var engineDir: Path

    init {
        // create .cache directory
        val cacheDir = Paths.get(".cached")
        if (!Files.exists(cacheDir)) Files.createDirectory(cacheDir)
        // create engine directory
        engineDir = cacheDir.resolve(checkNotNull(this::class.simpleName))
        if (!Files.exists(engineDir)) Files.createDirectory(engineDir)

        info("current .cached directory: $engineDir")
    }

    /* field */
    private var ratings = mutableMapOf<Long, MutableMap<Int, Double>>()
    private var latentUsers = mapOf<Long, DoubleArray>()
    private var latentNeighbors = mapOf<Long, DoubleArray>()
    private var meanOverall = 0.0
    private var stdOverall = 0.0
    private var users = mutableMapOf<Long, User>()
    private var items = mutableMapOf<Int, Item>()

    /* should be constant page 103 line 20 */
    private var mapDiffNeighbors = mutableMapOf<Long, MutableMap<Int, Double>>() // <User, <Item, r_vi - b_vi>>

    /* init field */
    private fun initRatings() {
        transaction {
            meanOverall =
                GamesDAO.slice(GamesDAO.PlaytimeForever.avg()).selectAll().first()[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero()
            stdOverall =
                GamesDAO.slice(GamesDAO.PlaytimeForever.function("STD")).selectAll().first()[GamesDAO.PlaytimeForever.function(
                    "STD"
                )].toDoubleOrZero()

            measureBlock("Retrieve all ratings") {
                GamesDAO.slice(GamesDAO.SteamId, GamesDAO.AppId, GamesDAO.PlaytimeForever)
                    .select { GamesDAO.PlaytimeForever.isNotNull() }
            }.forEach {
                ratings.putIfAbsent(it[GamesDAO.SteamId], mutableMapOf())
                ratings[it[GamesDAO.SteamId]]!![it[GamesDAO.AppId]] =
                    it[GamesDAO.PlaytimeForever].toDouble()
            }
            assertTrue { ratings.all { it.value.all { it.value.isFinite() } } }
        }
    }

    private fun initDataStructureForTraining() {


        users = transaction {
            measureBlock("Retrieve all users information") {
                GamesDAO.slice(
                    GamesDAO.SteamId,
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                ).select { GamesDAO.PlaytimeForever.isNotNull() }.groupBy(GamesDAO.SteamId)
            }.associate {
                it[GamesDAO.SteamId] to
                        User(
                            it[GamesDAO.SteamId],
                            it[GamesDAO.PlaytimeForever.avg()]!!.toDouble(),
                            it[GamesDAO.PlaytimeForever.function("STD")]!!.toDouble()
                        )
            }.toMutableMap()
        }

        items = transaction {
            measureBlock("Retrieve all items information") {
                GamesDAO.slice(
                    GamesDAO.AppId,
                    GamesDAO.PlaytimeForever.avg(),
                    GamesDAO.PlaytimeForever.function("STD")
                ).select { GamesDAO.PlaytimeForever.isNotNull() }.groupBy(GamesDAO.AppId)
            }.associate {
                it[GamesDAO.AppId] to
                        Item(
                            it[GamesDAO.AppId],
                            it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero(),
                            it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrOne()
                        )
            }.toMutableMap()
        }

        // init random
        val random = Random(System.currentTimeMillis())
        // Initialize factors of users
        latentUsers = measureBlock("Initialize random latent users factors")
        {
            users.asSequence().associate {
                it.key to generateDoubleArray(factors) { random.nextDouble() }
            }
        }
        assert(latentUsers.values.all
        { it.size == factors })

        latentNeighbors = measureBlock("Initialize random latent users factors")
        {
            users.asSequence().associate {
                it.key to generateDoubleArray(factors) { random.nextDouble() }
            }
        }
        assert(latentUsers.values.all
        { it.size == factors })


        /* bias should be constant ?
            page 103 line 20
         */
        measureBlock("Initialize neighbors static information")
        {

            val mapBiasNeighbors = mutableMapOf<Long, MutableMap<Int, Double>>()
            for (user in ratings) {
                for (item in user.value) {
                    mapBiasNeighbors.putIfAbsent(user.key, mutableMapOf())
                    mapBiasNeighbors[user.key]!![item.key] =
                        meanOverall + items[item.key]!!.avg + users[user.key]!!.avg
                }
            }
            assertTrue { mapBiasNeighbors.all { it.value.all { it.value.isFinite() } } }

            for (user in ratings) {
                for (item in user.value) {
                    mapDiffNeighbors.putIfAbsent(user.key, mutableMapOf())
                    mapDiffNeighbors[user.key]!![item.key] =
                        item.value - mapBiasNeighbors[user.key]!![item.key]!!
                }
            }
            assertTrue { mapDiffNeighbors.all { it.value.all { it.value.isFinite() } } }
        }

    }

    init {
        // start database connection
        var database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")



        if (File(engineDir.toString()).list { _, name -> name.endsWith("cache") }!!.count() > 0) {
            //trained
            measureBlock("Load learned parameter") { loadLearn() }
        } else {
            initRatings()
            initDataStructureForTraining()
            train()
        }
    }


    override fun saveLearn() {
        val propertiesFile = engineDir.resolve("properties.cache")
        val usersFile = engineDir.resolve("users.cache")
        val itemsFile = engineDir.resolve("items.cache")
        val latentUsersFile = engineDir.resolve("latentUsers.cache")
        val latentNeighborsFile = engineDir.resolve("latentNeighbors.cache")

        File(propertiesFile.toString()).printWriter().use { writer ->
            writer.println(meanOverall)
        }

        File(usersFile.toString()).printWriter().use { writer ->
            users.values.forEach { writer.println("${it.id}:${it.avg}") }
        }
        File(itemsFile.toString()).printWriter().use { writer ->
            items.values.forEach { writer.println("${it.id}:${it.avg}") }
        }
        File(latentUsersFile.toString()).printWriter().use { writer ->
            latentUsers.forEach { writer.println("${it.key}:${it.value.joinToString { "$it" }}") }
        }
        File(latentNeighborsFile.toString()).printWriter().use { writer ->
            latentNeighbors.forEach { writer.println("${it.key}:${it.value.joinToString { "$it" }}") }
        }

    }


    override fun loadLearn() {


        val propertiesFile = engineDir.resolve("properties.cache")
        val usersFile = engineDir.resolve("users.cache")
        val itemsFile = engineDir.resolve("items.cache")
        val latentUsersFile = engineDir.resolve("latentUsers.cache")
        val latentNeighborsFile = engineDir.resolve("latentNeighbors.cache")

        File(propertiesFile.toString()).useLines {
            meanOverall = it.toString().toDouble()
        }

        users = File(usersFile.toString()).useLines {
            it.associate {
                val local = it.split(':')
                val id = local[0].toLong()
                val avg = local[1].toDouble()
                id to User(id, avg, 0.0)
            }.toMutableMap()
        }

        items = File(itemsFile.toString()).useLines {
            it.associate {
                val local = it.split(':')
                val id = local[0].toInt()
                val avg = local[1].toDouble()
                id to Item(id, avg, 0.0)
            }.toMutableMap()
        }

        latentUsers = File(latentUsersFile.toString()).useLines {
            it.associate {
                val local = it.split(':')
                val userId = local[0].toLong()
                val values = local[1].split(",")
                val array = DoubleArray(values.count())
                values.forEachIndexed { index, value -> array.set(index, value.toDouble()) }
                userId to array
            }
        }

        latentNeighbors = File(latentNeighborsFile.toString()).useLines {
            it.associate {
                val local = it.split(':')
                val itemId = local[0].toLong()
                val values = local[1].split(",")
                val array = DoubleArray(values.count())
                values.forEachIndexed { index, value -> array.set(index, value.toDouble()) }
                itemId to array
            }
        }

    }



    override fun train() {
        val start = LocalDateTime.now()

        var currentError = 0.0
        var beforeError = 0.0

        val reversedRatings: MutableMap<Int, MutableMap<Long, Double>> = mutableMapOf()
        for (user in ratings) {
            for (item in user.value) {
                reversedRatings.putIfAbsent(item.key, mutableMapOf())
                reversedRatings[item.key]!![user.key] = item.value
            }
        }


        for (iteration in 1..iterations) {

            /* convergence */
            if (iteration != 1 && abs(currentError - beforeError) < errorTollerance) break

            var sumError = 0.0
            var learningRate = learningRate
            var count = reversedRatings.size

            reversedRatings.forEach { entry ->
                val itemId = entry.key
                info("start learning ${entry.value.size} ratings for item ${itemId}")

                /* independent parameter  */
                var independentFromU = DoubleArray(factors) { 0.0 }
                val usersThatRatedItem = reversedRatings[itemId]!!
                val neighbors = usersThatRatedItem.keys.toList()
                val normalizer = neighbors.size.toDouble() //.pow(-0.5)
                for (neighbor in neighbors) {
                    val prod = latentNeighbors[neighbor]!!.scalarProduct(mapDiffNeighbors[neighbor]!![itemId]!!)
                    independentFromU = independentFromU.updateIndexed { index, value -> value + prod[index] }
                }
                independentFromU = independentFromU.updateIndexed { index, value -> value / normalizer }
                assertTrue("independentFromU") { independentFromU.all { it.isFinite() } }
                /* independent parameter  */


                val internalItem = items[itemId]!!

                entry.value.forEach {
                    val internalUser = users[it.key]!!
                    val rate = it.value
                    val prediction = Latent2_RatingCalculator(
                        meanOverall,
                        internalUser.avg,
                        internalItem.avg,
                        latentUsers[it.key]!!,
                        independentFromU
                    ).calculate()

                    val error = rate - prediction


                    //train
                    trainBiasAndUserlatent(
                        internalUser,
                        internalItem,
                        error,
                        latentUsers[internalUser.id]!!,
                        independentFromU
                    )
                    trainLatentNeighbors(error, neighbors, independentFromU, latentUsers[internalUser.id]!!)

                    sumError += error

                }

                info("learning complete for item ${itemId}. Remains ${--count} ")
            }


            beforeError = currentError
            currentError = sumError / reversedRatings.values.count()

            info("error $currentError at iteration $iteration")

            learningRate -= decreaseLearningRateOf
        }


        val end = LocalDateTime.now()
        info("started at $start - ended at $end} - duration ${Duration.between(start, end).toMinutes()} minutes")
        saveLearn()

    }


    private fun trainBiasAndUserlatent(
        internalUser: User,
        internalItem: Item,
        error: Double,
        latentUser: DoubleArray,
        independentFromU: DoubleArray
    ) {
        // Get global information about dataset
        var biasUser = internalUser.avg
        var biasItem = internalItem.avg

        // b_u
        biasUser += learningRate * (error - lambda1 * biasUser)
        internalUser.avg = biasUser

        // b_i
        biasItem += learningRate * (error - lambda1 * biasItem)
        internalItem.avg = biasItem

        // update
        users[internalUser.id] = internalUser
        items[internalItem.id] = internalItem


        // p_u

        latentUser
            .forEachIndexed { index, value ->
                latentUsers[internalUser.id]!![index] =
                    value + learningRate * (error * independentFromU[index] - lambda1 * value)
            }

    }


    private fun trainLatentNeighbors(
        error: Double,
        neighbors: List<Long>,
        independentFromU: DoubleArray,
        latentUser: DoubleArray
    ) {
        // z_v
        for (neighbor in neighbors) {
            latentNeighbors[neighbor]!!.forEachIndexed { index, value ->
                latentNeighbors[neighbor]!![index] =
                    value + learningRate * (error * latentUser[index] * independentFromU[index] - lambda1 * value)
            }
        }
    }

    override fun test(): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}