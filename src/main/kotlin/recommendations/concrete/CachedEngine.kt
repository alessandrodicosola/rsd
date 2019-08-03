package recommendations.concrete

import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.Paths
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class CachedEngine<T : Number>(private val engine: IRSEngine<T>) : IRSEngine<T>() {

    fun cached(id: T) = File(basePath.resolve("$id.txt").toString()).exists()

    override fun getRecommendations(id: Long): List<RSObject<T>> {

        val internalFile = basePath.resolve("$id.txt")
        val file = File(internalFile.toString())
        val cacheList: MutableList<RSObject<T>> = mutableListOf()

        if (file.exists()) {
            file.reader().use {
                it.forEachLine {
                    it.split(':').let {
                        (convertStringToNumber(it[0])) hasScore it[1].toDouble()
                    }
                }
            }
            return cacheList
        } else {
            val inList = engine.getRecommendations(id)
            file.writer().use { writer ->
                inList.forEach { writer.write(it.toString()) }
            }
            return inList
        }
    }

    private val basePath =
        Paths.get("").toAbsolutePath().resolve(".cached/${engine::class.simpleName}")

    private fun convertStringToNumber(string: String): Number {
        val listOfNumber = listOf<Number?>(
            string.toDoubleOrNull(),
            string.toIntOrNull()
        )

        return listOfNumber.filterNotNull().first()
    }
}