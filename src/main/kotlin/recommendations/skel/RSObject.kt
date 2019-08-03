package recommendations.skel

import java.io.Serializable
import kotlin.reflect.full.isSubclassOf

open class RSObject<T : Number>(val id: T, val score: Double) {

    override fun toString(): String {
        return "$id:$score"
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + score.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RSObject<*>) return false

        if (id != other.id) return false
        if (score != other.score) return false

        return true
    }
}

infix fun <T : Number> T.hasScore(that: Double): RSObject<T> = RSObject(this, that)
