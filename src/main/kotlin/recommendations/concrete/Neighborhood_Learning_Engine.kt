package recommendations.concrete

import common.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.*
import sql.dao.GamesDAO
import sql.dao.GamesTestDAO
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/**
 * Ratings are calculated with a trained recommendation system
 * @param factors Max factors to extract
 * @param iterations Max iterations allowed
 * @param learningRate
 * @param decreaseLearningRateOf
 * @param lambda1
 */
class Neighborhood_Learning_Engine(
    private val iterations: Int,
    private val factors: Int,
    private val learningRate: Double,
    private val decreaseLearningRateOf: Double,
    private val lambda1: Double,
    private val errorTollerance: Double
) : IRSEngine<Long, Int, Double>(), ITrainable, ITestable<Double> {
    override fun updateRecommendations(userId: Long, itemId: Int, ratingType: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // constraint on parameter
    init {
        assert(decreaseLearningRateOf < learningRate)
    }

    private var meanOverall: Double = 0.0
    /// Map<SteamId,List<(AppId,Rating)>
    private var ratings = mutableMapOf<Long, MutableMap<Int, Double>>()

    private var users = mutableMapOf<Long, User>()
    private var items = mutableMapOf<Int, Item>()

    private var latentUsers: MutableMap<Long, DoubleArray> = mutableMapOf()
    private var latentItems: MutableMap<Int, DoubleArray> = mutableMapOf()
    /// Map<Long,List<AppId,List<Factor>>>
    private var latentImplicits: MutableMap<Long, MutableMap<Int, DoubleArray>> = mutableMapOf()

    private var engineDir: Path

    init {

        // create .cache directory
        val cacheDir = Paths.get(".cached")
        if (!Files.exists(cacheDir)) Files.createDirectory(cacheDir)
        // create engine directory
        engineDir = cacheDir.resolve(checkNotNull(this::class.simpleName))
        if (!Files.exists(engineDir)) Files.createDirectory(engineDir)

        info("current .cached directory: $engineDir")


        // start database connection
        var database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")



        if (File(engineDir.toString()).list { _, name -> name.endsWith("cache") }!!.count() > 0) {
            //trained
            measureBlock("Load learned parameter") { loadLearn() }
        } else {
            //not trained
            initRatings()
            initDataStructureForTraining()
        }
    }


    /**
     * Implementazione del metodo [ITestable.test] calcolando l'errore attraverso il [RMSECalculator]
     */
    override fun test(): Double {
        //Ottengo i dati di test dal database
        val testRatings = mutableMapOf<Long, MutableMap<Int, Double>>()
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

        val excludedItems = mutableMapOf<Long, Int>()
        val calculatedRatings: MutableMap<Long, MutableMap<Int, Double>> = mutableMapOf()
        for (entry in testRatings) {
            val userId = entry.key
            for (item in entry.value) {
                val itemId = item.key


                val latentUser = latentUsers[userId]
                val latentItem = latentItems[itemId]
                val latentImplicit = latentImplicits[userId]

                /*
                In alcuni casi alcuni utenti presenti nel test set non sono presenti nel training set
                quindi non si hanno informazioni associate su di essi. Percui vengono saltati
                */
                if (latentUser == null || latentItem == null || latentImplicit == null) {
                    warning("rate given by $userId to $itemId exclude from the test")
                    excludedItems[userId] = itemId
                    continue
                }
                //Calcolo la predizione del rate
                val rate = Latent_RatingCalculator(
                    meanOverall,
                    users[userId]!!.avg,
                    items[itemId]!!.avg,
                    latentUser,
                    latentItem,
                    latentImplicit
                ).calculate()

                if (!calculatedRatings.containsKey(userId)) calculatedRatings[userId] = mutableMapOf()

                calculatedRatings[userId]!!.put(itemId, rate)
            }
        }


        //Elimino dal test set gli oggetti degli utenti saltati
        for (exclude in excludedItems) {
            if (testRatings.contains(exclude.key)) {
                if (testRatings[exclude.key]!!.contains(exclude.value)) {
                    testRatings[exclude.key]!!.remove(exclude.value)
                }
            }
        }

        val trueRatings = testRatings.flatMap { it.value.values }.toDoubleArray()
        val predictedRatings = calculatedRatings.flatMap { it.value.values }.toDoubleArray()
        //Calcolo il root mean square error
        return RMSECalculator(trueRatings, predictedRatings).calculate()
    }
    /**
     * Metodo che inizializza i rate degli utenti
     */
    private fun initRatings() {
        transaction {
            measureBlock("Retrieve all ratings") {
                GamesDAO.slice(GamesDAO.SteamId, GamesDAO.AppId, GamesDAO.PlaytimeForever)
                    .select { GamesDAO.PlaytimeForever.isNotNull() }
            }.forEach {
                if (!ratings.containsKey(it[GamesDAO.SteamId])) ratings[it[GamesDAO.SteamId]] = mutableMapOf()

                ratings[it[GamesDAO.SteamId]]!!.put(
                    it[GamesDAO.AppId],
                    it[GamesDAO.PlaytimeForever].toDoubleOrZero()
                )
            }
        }
    }

    /**
     * Metodo che inizializza le strutture [users] [items] [latentUsers] [latentItems] [latentImplicits]
     */
    private fun initDataStructureForTraining() {

        transaction {
            val listOfAvg =
                GamesDAO.slice(GamesDAO.PlaytimeForever.avg()).selectAll()
                    .map { it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero() }
            assert(listOfAvg.size == 1,
                { error("SELECT AVG(LOG_PLAYTIME) FROM GAMES_TRAINIG returns size=${listOfAvg.size} expected size=1") })
            meanOverall = listOfAvg.first()
        }

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
                            it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrOne()
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
                                    it[GamesDAO.PlaytimeForever.function("STD")].toDoubleOrOne()
                                )
                    }.toMutableMap()
            }
        }

        // init random
        val random = Random(System.currentTimeMillis())
        // Initialize factors of users
        latentUsers = measureBlock("Initialize random latent users factors") {
            users.asSequence().associate {
                it.key to generateDoubleArray(factors) { random.nextDouble() }
            }.toMutableMap()
        }
        assert(latentUsers.values.all { it.size == factors })


        //Initialize factors of items
        latentItems = measureBlock("Initialize random latent items factors")
        {
            items.asSequence().associate {
                it.key to generateDoubleArray(factors) { random.nextDouble() }
            }.toMutableMap()
        }
        assert(latentItems.values.all { it.size == factors })

        // Initialize factors of implicits
        latentImplicits = measureBlock("Initialize random latent implicit factors")
        {
            ratings.mapValues {
                it.value.mapValues { generateDoubleArray(factors) { random.nextDouble() } }.toMutableMap()
            }.toMutableMap()
        }
        assert(latentImplicits.all { it.value.all { it.value.size == factors } })
    }

    /**
     * //Implementazione del metodo [ITrainable.train]
     */
    override fun train() {
        val start = LocalDateTime.now()

        //Ottengo il numero massimo di rate da calcolare per ogni iterazione
        val ratingsCount = ratings.asSequence().map { it.value.size }.sum()
        //Ottengo il numero massimo di utenti nel datasetr
        val userCount = ratings.asSequence().map { it.key }.distinct().count()

        info("Need to train $userCount users and $ratingsCount ratings")

        var currentStep = 0

        var currentError = 0.0
        var beforeError = 0.0

        for (iteration in 1..iterations) {

            info("error $currentError at iteration $iteration")

            /* if convergence */
            if (iteration != 1 && abs(currentError - beforeError) < errorTollerance) break

            var sumError = 0.0

            var learningRate = learningRate

            //Eseguo la discesa del gradiente
            for (element in ratings) {

                for (item in element.value) {
                    //Ottengo l'utente
                    val internalUser: User = users[element.key]!!
                    //Otteno l'oggetto
                    val internalItem: Item = items[item.key]!!
                    //Ottengo il rate dato dall'utente all'oggetto
                    val rate = item.value

                    var error: Double = 0.0

                    //Calcolo la predizione
                    val prediction = Latent_RatingCalculator(
                        meanOverall,
                        internalUser.avg,
                        internalItem.avg,
                        //Vettore contenente i fattori latenti dell'utente
                        latentUsers[internalUser.id]!!,
                        //Vettore contenente i fattori latenti dell'oggetto
                        latentItems[internalItem.id]!!,
                        //Vettore contenente i fattori latenti delle informazioni implicite relative a un utente
                        latentImplicits[internalUser.id]!!
                    ).calculate()

                    //Calcolo l'errore
                    error = rate - prediction

                    //Eseguo il training.
                    trainUserItem(
                        internalUser,
                        internalItem,
                        error,
                        latentUsers,
                        latentItems,
                        latentImplicits[internalUser.id]!!
                    )
                    //Riduco il learning rate per garantire la convergenza della discesa del gradiente
                    learningRate -= decreaseLearningRateOf

                    // Aggiorno l'errore
                    sumError += error

                    // log
                    currentStep++
                }
            }

            beforeError = currentError
            // Normalizzo l'errore
            currentError = sumError / ratingsCount
        }
        val end = LocalDateTime.now()
        info("started at $start - ended at $end} - duration ${Duration.between(start, end).toMinutes()} minutes")
        //Salvo i parametri
        saveLearn()
    }

    /**
     * //Implementazione del metodo [ITrainable.saveLearn]
     */
    override fun saveLearn() {
        val usersFile = engineDir.resolve("users.cache")
        val itemsFile = engineDir.resolve("items.cache")
        val latentUsersFile = engineDir.resolve("latentUsers.cache")
        val latentItemsFile = engineDir.resolve("latentItems.cache")
        val latentImplicitsFile = engineDir.resolve("latentImplicits.cache")

        File(usersFile.toString()).printWriter().use { writer ->
            users.values.forEach { writer.println("${it.id}:${it.avg}") }
        }
        File(itemsFile.toString()).printWriter().use { writer ->
            items.values.forEach { writer.println("${it.id}:${it.avg}") }
        }
        File(latentUsersFile.toString()).printWriter().use { writer ->
            latentUsers.forEach { writer.println("${it.key}:${it.value.joinToString { "$it" }}") }
        }
        File(latentItemsFile.toString()).printWriter().use { writer ->
            latentItems.forEach { writer.println("${it.key}:${it.value.joinToString { "$it" }}") }
        }
        File(latentImplicitsFile.toString()).printWriter().use { writer ->
            latentImplicits.forEach {
                val userId = it.key
                it.value.forEach {
                    val itemId = it.key
                    writer.println("$userId-$itemId:${it.value.joinToString { "$it" }}")
                }
            }
        }
    }

    /**
     * //Implementazione del metodo [ITrainable.loadLearn]
     */
    override fun loadLearn() {
        transaction {
            val listOfAvg =
                GamesDAO.slice(GamesDAO.PlaytimeForever.avg()).selectAll()
                    .map { it[GamesDAO.PlaytimeForever.avg()].toDoubleOrZero() }
            assert(listOfAvg.size == 1,
                { error("SELECT AVG(LOG_PLAYTIME) FROM GAMES_TRAINIG returns size=${listOfAvg.size} expected size=1") })
            meanOverall = listOfAvg.first()
        }

        val usersFile = engineDir.resolve("users.cache")
        val itemsFile = engineDir.resolve("items.cache")
        val latentUsersFile = engineDir.resolve("latentUsers.cache")
        val latentItemsFile = engineDir.resolve("latentItems.cache")
        val latentImplicitsFile = engineDir.resolve("latentImplicits.cache")

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
            }.toMutableMap()
        }

        latentItems = File(latentItemsFile.toString()).useLines {
            it.associate {
                val local = it.split(':')
                val itemId = local[0].toInt()
                val values = local[1].split(",")
                val array = DoubleArray(values.count())
                values.forEachIndexed { index, value -> array.set(index, value.toDouble()) }
                itemId to array
            }.toMutableMap()
        }


        latentImplicits = File(latentImplicitsFile.toString()).useLines {
            it.associate {
                val local = it.split(":")
                val user_item = local[0].split("-")
                val userId = user_item[0].toLong()
                val itemId = user_item[1].toInt()
                val values = local[1].split(',')
                val array = DoubleArray(values.count())
                values.forEachIndexed { index, value -> array.set(index, value.toDouble()) }
                userId to mutableMapOf(itemId to array)
            }.toMutableMap()
        }

    }

    /**
     * Metodo che esegue il training di uno specifico utente per un specifico oggetto andando ad aggiornare le struttre dati per il training le quali sono [users] [items] [latentUsers] [latentItems] [latentImplicits]
     */
    private fun trainUserItem(
        user: User,
        item: Item,
        error: Double,
        latentUsers: MutableMap<Long, DoubleArray>,
        latentItems: MutableMap<Int, DoubleArray>,
        latentImplicits: MutableMap<Int, DoubleArray>
    ) {

        // Get global information about dataset
        var biasUser = user.avg
        var biasItem = item.avg

        // b_u
        biasUser += learningRate * (error - lambda1 * biasUser)
        user.avg = biasUser

        // b_i
        biasItem += learningRate * (error - lambda1 * biasItem)
        item.avg = biasItem

        //Aggiorno i valori dell'utente e dell'oggetto
        users[user.id] = user
        items[item.id] = item

        //Calcolo il fattore per normalizzare la somma delle informazioni implicite rispetto a un utente
        val factorForNormalizingImplicits = latentImplicits.count().toDouble().pow(-0.5)

        //Aggiorno le informazioni dei fattori latenti per lo specifico oggetto
        latentItems[item.id] =
            latentItems[item.id]!!.asSequence()
                .mapIndexed { index, value -> value + learningRate * (error * (latentItems[item.id]!![index] + factorForNormalizingImplicits * latentImplicits.map { it.value[index] }.sumByDouble { it })) - lambda1 * value }
                .toList().toDoubleArray()

        //Aggiorno le informazioni dei fattori latenti per uno specifico utente
        latentUsers[user.id] = latentUsers[user.id]!!.asSequence()
            .mapIndexed { index, value -> value + learningRate * (error * latentItems[item.id]!![index] - lambda1 * value) }
            .toList().toDoubleArray()

        //Aggiorno le informazioni sui fattori latenti delle informazioni implicite
        for (implicit in latentImplicits.keys) {
            latentImplicits[implicit] = latentImplicits[implicit]!!.asSequence()
                .mapIndexed { index, value -> value + learningRate * (error * latentItems[item.id]!![index] - lambda1 * value) }
                .toList().toDoubleArray()
        }
    }

    /**
     * Implmentazoine del [IRSEngine.getRecommendations]
     */
    override fun getRecommendations(userId: Long, _itemId: Int): List<RSObject<Int, Double>> {
        //Iniziallizzo i ratings
        initRatings()
        //Ottengo gli oggetti valutati dall'utente
        val itemsRatedByUser = ratings[userId]!!.map { it.key }
        //Ottengo tutti gli oggetto del dataset
        val allItems = items.keys
        //Ottengo gli oggetti non valutati dall'utente
        val itemsNotRatedByUser = allItems - itemsRatedByUser

        //Per ogni oggetto non valutato dall'utente calcolo la raccomandazione
        return itemsNotRatedByUser.map { itemId ->
            val prediction = Latent_RatingCalculator(
                meanOverall,
                users[userId]!!.avg,
                items[itemId]!!.avg,
                latentUsers[userId]!!,
                latentItems[itemId]!!,
                latentImplicits[userId]!!
            ).calculate()

            RSObject(itemId, prediction)
        }

    }


}