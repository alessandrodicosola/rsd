package recommendations.concrete

import common.info
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * [IRSEngine] che calcola le raccomandazioni e le salva su disco.
 * Successivamente vengono caricate dal disco senza ricalcolarle.
 * @param engine [IRSEngine] da utilizzare
 */
class CachedEngine<ObjKey : Number, Item : Number, Value : Number>(private val engine: IRSEngine<ObjKey, Item, Value>) :
    IRSEngine<ObjKey, Item, Value>() {

    override fun updateRecommendations(userId: ObjKey, itemId: Item, ratingType: Value) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    init {
        // create .cache directory
        val cacheDir = Paths.get(".cached")
        if (!Files.exists(cacheDir)) Files.createDirectory(cacheDir)
        // create engine directory
        val engineDir = cacheDir.resolve(checkNotNull(engine::class.simpleName))
        if (!Files.exists(engineDir)) Files.createDirectory(engineDir)

        info("current .cached directory: $engineDir")

    }

    private val basePath = Paths.get(".cached", engine::class.simpleName).toAbsolutePath()

    /**
     * @return Restituisce [True] se il sistema ha già salvato le raccomandazioni altrimenti [False]
     */
    fun cached(id: ObjKey) = File(basePath.resolve("$id.txt").toString()).exists()

    override fun getRecommendations(userId: ObjKey, _itemId: Item): List<RSObject<Item, Value>> {

        //Ottengo il percorso del file su cui salvare le raccomandazioni.
        val internalFile = basePath.resolve("$userId.txt")
        //Ottengo il file
        val file = File(internalFile.toString())

        val cacheList: MutableList<RSObject<Item, Value>> = mutableListOf()

        //Se il file esiste carico le raccomandazioni
        if (file.exists()) {
            file.reader().use {
                it.forEachLine {
                    it.split(':').let {
                        cacheList.add(convertStringToNumber(it[0]) as Item hasScore it[1] as Value)
                    }
                }
            }
            return cacheList

        }
        //Se il file non esiste eseguo l'engine per calcolare le raccomandazioni
        else {
            val inList = engine.getRecommendations(userId,_itemId)
            file.createNewFile()
            file.writer().use { writer ->
                inList.forEach {
                    writer.write(it.toString())
                    writer.write("\n")
                }
            }
            return inList
        }
    }


    private fun convertStringToNumber(string: String): Number {
        val listOfNumber = listOf<Number?>(
            string.toIntOrNull(),
            string.toDoubleOrNull()
        )

        return listOfNumber.filterNotNull().first()
    }
}