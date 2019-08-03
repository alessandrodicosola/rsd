package recommendations.skel

abstract class IRSEngine<Key : Number> {
    abstract fun getRecommendations(id: Long): List<RSObject<Key>>
}