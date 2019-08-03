package recommendations.concrete

import logging.info
import recommendations.skel.IRSEngine
import recommendations.skel.RSObject
import recommendations.skel.hasScore
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

class CachedEngine<T : Number>(private val engine: IRSEngine<T>) : IRSEngine<T>() {
    init {
        // create .cache directory
        val cacheDir = Paths.get(".cached")
        if (!Files.exists(cacheDir)) Files.createDirectory(cacheDir)
        // create engine directory
        val engineDir = cacheDir.resolve(checkNotNull(engine::class.simpleName))
        if (!Files.exists(engineDir)) Files.createDirectory(engineDir)
    }

    private val basePath = Paths.get(".cached", engine::class.simpleName).toAbsolutePath()

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
            string.toDoubleOrNull(),
            string.toIntOrNull()
        )

        return listOfNumber.filterNotNull().first()
    }
}