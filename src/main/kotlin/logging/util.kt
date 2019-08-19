package logging

import java.util.logging.Logger
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

inline fun <T> Any.measureBlock(message: String, block: () -> T): T {
    val now = System.currentTimeMillis()
    val out = block()
    val end = System.currentTimeMillis()
    val span = end - now
    val flag = span > 1000
    Logger.getLogger(this::class.simpleName)
        .info("$message => Elapsed time: ${if (flag) span / 1000 else span} ${if (flag) "ms" else "s"}")
    return out
}


inline fun <T> Any.measureBlock(block: () -> T): T {
    return measureBlock("", block)
}

fun Number?.toDoubleOrZero(): Double {
    return this?.toDouble() ?: 0.0;
}

fun Any.info(message: String) {
    Logger.getLogger(this::class.simpleName).info(message)
}

