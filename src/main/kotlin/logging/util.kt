package logging

import java.util.logging.Logger
import kotlin.jvm.internal.Reflection
import kotlin.reflect.KClass

inline fun <T> Any.measureBlock(block: () -> T): T {
    val now = System.currentTimeMillis()
    val out = block()
    val end = System.currentTimeMillis()
    Logger.getLogger(this::class.simpleName).info("Elapsed time: ${(end - now) / 1000}")
    return out
}

