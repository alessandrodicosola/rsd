package recommendations.skel

abstract class IRSEngine() {
    abstract fun getRecommendations(userId: Long): List<RSObject>
}