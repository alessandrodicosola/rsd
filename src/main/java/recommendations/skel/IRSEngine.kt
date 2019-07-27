package recommendations.skel

abstract class IRSEngine(iRatingCalculator: IRatingCalculator) {
    abstract fun getRecommendations(userId: Long): List<RSObject>
}