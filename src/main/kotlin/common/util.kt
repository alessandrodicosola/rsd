package common

import org.jetbrains.exposed.sql.Seq
import java.util.logging.Logger
import kotlin.random.Random

inline fun <T, reified K : Any> K.measureBlock(message: String, block: () -> T): T {

    val now = System.currentTimeMillis()
    val out = block()
    val end = System.currentTimeMillis()
    val span = end - now
    val flag = span > 1000
    Logger.getLogger(this::class.simpleName)
        .info("$message => Elapsed time: ${if (flag) span / 1000 else span} ${if (flag) "s" else "ms"}")
    return out
}


inline fun <T, reified K : Any> K.measureBlock(block: () -> T): T {
    return this.measureBlock("", block)
}

fun Number?.toDoubleOrZero(): Double {
    return this?.toDouble() ?: 0.0;
}

inline fun <reified T : Any> T.info(message: String) {
    Logger.getLogger(this::class.simpleName).info(message)
}

inline fun <reified T : Any> T.info(noinline block: () -> String) {
    Logger.getLogger(this::class.simpleName).info(block)
}


fun generateDoubleArray(count: Int, generator: (Int) -> Double): DoubleArray {
    return DoubleArray(count, generator);
}

