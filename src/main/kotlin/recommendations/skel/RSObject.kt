package recommendations.skel

import java.io.Serializable

open class RSObject<T : Number>(val id: T, val score: Double) {
    override fun toString(): String {
        return "$id:$score"
    }
}

infix fun <T : Number> T.hasScore(that: Double): RSObject<T> = RSObject(this, that)
