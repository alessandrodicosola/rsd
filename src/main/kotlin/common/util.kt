package common

import java.time.Duration
import java.time.LocalTime
import java.util.logging.Logger

inline fun <T, reified K : Any> K.measureBlock(message: String, block: () -> T): T {

    val start = LocalTime.now()
    val out = block()
    val end = LocalTime.now()
    val span = Duration.between(start, end).toMillis()
    this.info("$message => time: $span ms")
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

inline fun <reified T : Any> T.warning(message: String) {
    Logger.getLogger(this::class.simpleName).warning(message)
}


fun generateDoubleArray(count: Int, generator: (Int) -> Double): DoubleArray {
    return DoubleArray(count, generator);
}

