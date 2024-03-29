package recommendations.skel

/**
 * Classe che definisce la raccomandazione
 * @param id Id dell'oggetto
 * @param score Valore della rate calcolato dal [IRSEngine]
 */
open class RSObject<out T : Number, out O : Number>(val id: T, val score: O) {

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
        if (other !is RSObject<*, *>) return false

        if (id != other.id) return false

        return true
    }
}

infix fun <T : Number, O : Number> T.hasScore(that: O): RSObject<T, O> = RSObject(this, that)
