package recommendations.skel

abstract class IRSEngine<Key> {
    abstract fun getRecommendations(id: Long): List<RSObject<Key>>
}