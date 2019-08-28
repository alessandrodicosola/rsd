package recommendations.skel

interface IRatingCalculator<out RatingType> {
    fun calculate(): RatingType
}