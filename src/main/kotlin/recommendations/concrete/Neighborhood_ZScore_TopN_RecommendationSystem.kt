package recommendations.concrete

import common.info
import common.measureBlock
import common.toDoubleOrZero
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import recommendations.skel.*
import sql.dao.GamesDAO
import sql.dao.GamesTestDAO


/**
 * Simple Neighborhood Recommendation System which retrieves recommendations with
 * - ZScore for calculating ratings
 * - TopN Neighbors for selecting neighbors
 * - Persona Correlation weighted with a factor
 * - Each game is indexed with an Int
 * @param numberOfNeighbors Numero di vicini da utilizzare per la raccomandazione
 * @param factorForNormalizeWeight Fattore che incrementa la somiglianza tra due utenti se hanno vlutate almeno un numero di oggetti uguale o maggiore di [factorForNormalizeWeight]
 */
class Neighborhood_ZScore_TopN_RecommendationSystem(
    private val numberOfNeighbors: Int,
    private val factorForNormalizeWeight: Int
) :
    IRSEngine<Long, Int, Double>(), ITestable<Double> {


    override fun updateRecommendations(userId: Long, itemId: Int, ratingType: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //Ratings nel dataset
    private var ratings: MutableMap<Long, MutableMap<Int, Double>> = mutableMapOf()
    //Mappa <ID,Utente>
    private var users: MutableMap<Long, User> = mutableMapOf()
    //Mappa <ID,Oggetti>
    private var items: MutableMap<Int, Item> = mutableMapOf()

    init {
        var database: Database =
            Database.connect("jdbc:mysql://127.0.0.1:3306/steam", "com.mysql.jdbc.Driver", "root", "")

        initRatings()
        initDataStructures()
    }

    /**
     * Metodo che inizializza i rates degli utenti
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
     * Metodo che inizializza le strutture [users] [items]
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

    /**
     * Implementazione del [IRSEngine.getRecommendations]
     */
    override fun getRecommendations(userId: Long, itemId: Int): List<RSObject<Int, Double>> {


        // 0.Ottengo le informazioni dell'utente
        val user = users[userId]!!

        // 1.Ottengo per ogni oggetto non valutato dall'utente tutti i vicini simili all'utente a cui raccomandare l'oggetto
        val itemsAndNeighbors = measureBlock("Getting neighbors for user: ${user.id}...") {
            getItemsAndNeighbors(user)
        }

        // 2.Per ogni oggetto calcolo il rate
        val ratings = measureBlock("Calculating ratings...") {
            getRatings(user, itemsAndNeighbors)
        }

        // 3.Mostro List<RSObject>
        return ratings
    }

    /**
     * Metodo che calcola i vicini rispetto a un utente a cui deve essere raccomandato un oggetto
     * @param user Utente
     * @param itemId Id dell'oggetto
     * @param ratingsUser Rates dell'utente
     */
    private fun getNeighborsForItem(
        user: User,
        itemId: Int,
        ratingsUser: MutableMap<Int, Double>
    ): List<Neighbor> {
        //Ottengo tutti gli oggetti valutati dall'utente
        val userItems = ratingsUser.map { it.key }
        //Variabile contente i vicini calcolati
        val outNeighbors = mutableListOf<Neighbor>()

        //Filtro i ratings selezionando solo gli utenti che hanno valutato l'oggetto definito da itemId
        for (entry in ratings.filter { it.value.containsKey(itemId) }) {
            //Id del vicino
            val neighborId = entry.key
            //Ottengo i rates del vicino
            val ratingsNeighbor = entry.value
            //Ottengo un set contenente gli id degli oggetti valutati dal vicino
            val itemsNeighbor = ratingsNeighbor.map { it.key }
            //Ottengo gli oggetti valutati da entrambi
            val itemsRatedByBoth = userItems.intersect(itemsNeighbor)

            if (itemsRatedByBoth.isNotEmpty()) {
                //Ottengo i dati del vicino
                val currentNeighbor = Neighbor(users[neighborId]!!, 0.0)
                //Ottengo i rates degli oggetti dell'utente valutati contemporaneamente dall'utente e dall'vicino
                val itemsU = ratingsUser.filter { itemsRatedByBoth.contains(it.key) }
                //Ottengo i rates degli oggetti del vicino valutati contemporaneamente dall'utente e dall'vicino
                val itemsV = entry.value.filter { itemsRatedByBoth.contains(it.key) }
                info("${user.id} and ${currentNeighbor.id} have rated both ${itemsU.size} items ")
                //Calcolo la somiglianza tra l'utente e il vicino
                currentNeighbor.weight = WeightedPersonaCorrelation(
                    itemsU,
                    itemsV,
                    user,
                    currentNeighbor,
                    factorForNormalizeWeight
                ).calculate()

                outNeighbors.add(currentNeighbor)
            }
        }
        // Restituisco vicini in numero pari a [numberOfNeighbors] ordinati in modo decrescente rispetto alla somiglianza con l'utente
        return outNeighbors.asSequence().filter { it.weight > 0 }.sortedByDescending { it.weight }
            .take(numberOfNeighbors)
            .toList()
    }

    /**
     * Metodo che calcola, per ogni oggetto non valutato dall'utente, i vicini più simili all'utente rispetto al particolare oggetto
     * @param user Utente
     */
    private fun getItemsAndNeighbors(user: User): Map<Int, List<Neighbor>> {
        //Ottengo i rates dell'utente
        val ratingsUser = ratings[user.id]!!
        //Ottengo il set contenente gli id degli oggetti valutati dall'utente
        val userItems = ratingsUser.asSequence().map { it.key }
        //Ottengo tutti gli oggetti
        val allItems = items.asSequence().map { it.key }
        //Ottengo il set contenente gli oggetti non valutati dall'utente
        val missingItems = allItems - userItems

        val outMap = mutableMapOf<Int, List<Neighbor>>()

        //Per ogni oggetto non valutato dall'utente ottengo i rispettivi vicini
        missingItems.forEach {
            outMap[it] = getNeighborsForItem(user, it, ratingsUser)
        }
        return outMap
    }

    /**
     * Metodo che calcola le raccomandazioni per un utente
     * @param user Utente
     * @param itemsAndNeighbors Insieme contenente per ogni oggetto, i vicini calcolati rispetto all'utente per il particolare oggetto.
     */
    private fun getRatings(
        user: User,
        itemsAndNeighbors: Map<Int, List<Neighbor>>
    ): List<RSObject<Int, Double>> {
        val listOut: MutableList<RSObject<Int, Double>> = mutableListOf()

        itemsAndNeighbors.forEach { entry ->
            val itemId = entry.key
            //Per ogni oggetto calcolo il rate
            val score = getRate(user, itemId, entry.value)
            listOut.add(itemId hasScore score)
        }
        //Restituisco la lista delle raccomandazioni
        return listOut
    }

    /**
     * Metodo che calcola il rate di un utente rispetto a un oggetto
     * @param user Utente
     * @param itemId Id dell'oggetto
     * @param neighbors Utenti simili all'utente rispetto al particolare oggetto
     */
    private fun getRate(user: User, itemId: Int, neighbors: List<Neighbor>): Double {
        //Ottengo il set dei rate dati dagli utenti simili all'utente designato per la raccomandazione rispetto al particolare oggetto
        //<NeighborId,Rate>
        val neighborsRating = neighbors.asSequence().associate {
            //Ottengo tutti i rates del vicino
            val map = ratings[it.id]!!
            //Prelevo il rate con id=itemId
            val rate = map[itemId]!!
            //Costruisco una nuova mappa fatta da <NeighborId,Rate>
            it.id to rate
        }
        //Calcolo il rate della raccomandazione
        return ZScoreRating(user, neighbors, neighborsRating).calculate()
    }

    /**
     * Implementazione del metodo [ITestable.test] calcolando l'errore attraverso il [RMSECalculator]
     */
    override fun test(): Double {

        val excludedItems = mutableListOf<Pair<Long, Int>>()
        val excludedUsers = mutableListOf<Long>()

        //Ottengo i dati di test dal database
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
        //Imposto un massimo di utenti su cui calcolare le raccomandazioni altrimenti il test impiega molto tempo poichè gli utenti nel dataset sono molti
        val maxUser = 30
        //Selezioni N utenti dal database
        val selectedUsers = (1..maxUser).map { testRatings.keys.random() }
        //Degli utenti selezionati uso soltanto quelli che presentano valutazioni su oggetti presenti nel test set
        testRatings = testRatings.filterKeys { selectedUsers.contains(it) }.toMutableMap()

        val calculatedRatings = mutableMapOf<Long, MutableMap<Int, Double>>()

        //Calcolo le raccomandazioni
        for (user in testRatings) {
            val selectedUser = users[user.key]!!
            /*
               In alcuni casi alcuni utenti presenti nel test set non sono presenti nel training set
               quindi non si hanno informazioni associate su di essi. Percui vengono saltati
            */
            if (ratings[selectedUser.id] == null) {
                excludedUsers.add(selectedUser.id)
                continue
            }

            for (item in user.value) {
                val userRatings = ratings[selectedUser.id]!!
                val neighbors = getNeighborsForItem(selectedUser, item.key, userRatings)
                val prediction = getRate(selectedUser, item.key, neighbors)

                //Evito gli le raccomandazioni calcolate rispetto ad un partiolare utente ed oggetto
                // in cui l'utente non presenta vicini causando un valore NaN
                if (!prediction.isFinite()) {
                    excludedItems.add(selectedUser.id to item.key)
                    continue
                }

                calculatedRatings.putIfAbsent(selectedUser.id, mutableMapOf())
                calculatedRatings[selectedUser.id]!![item.key] = prediction
            }
        }

        //Rimuovo dal test set tutti gli utenti saltati
        excludedUsers.forEach { testRatings.remove(it) }
        //Rimuovo dal test set tutti gli oggetti saltati
        testRatings.forEach { entry ->
            excludedItems.forEach {
                if (entry.key == it.first) entry.value.remove(it.second)
            }
        }


        val trueRatings = testRatings.asSequence().flatMap { it.value.values.asSequence() }.toList().toDoubleArray()
        val predictedRatings =
            calculatedRatings.asSequence().flatMap { it.value.values.asSequence() }.toList().toDoubleArray()
        //Calcolo il root mean square error
        return RMSECalculator(trueRatings, predictedRatings).calculate()
    }


}


