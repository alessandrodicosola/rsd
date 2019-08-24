package recommendations.concrete

import common.info
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class CachedEngine<ObjKey : Number, Item : Number, Value : Number>(private val engine: IRSEngine<ObjKey, Item, Value>) :
    IRSEngine<ObjKey, Item, Value>() {
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

    fun cached(id: ObjKey) = File(basePath.resolve("$id.txt").toString()).exists()

    override fun getRecommendations(id: ObjKey): List<RSObject<Item, Value>> {

        val internalFile = basePath.resolve("$id.txt")
        val file = File(internalFile.toString())
        val cacheList: MutableList<RSObject<Item, Value>> = mutableListOf()

        if (file.exists()) {
            file.reader().use {
                it.forEachLine {
                    it.split(':').let {
                        cacheList.add(convertStringToNumber(it[0]) as Item hasScore it[1] as Value)
                    }
                }
            }
            return cacheList
        } else {
            val inList = engine.getRecommendations(id)
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