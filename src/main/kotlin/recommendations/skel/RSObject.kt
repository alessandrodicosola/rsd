package recommendations.skel

open class RSObject<out T>(val id: T, val score: Double)
infix fun <Key> Key.hasScore(that: Double): RSObject<Key> = RSObject(this,that)
