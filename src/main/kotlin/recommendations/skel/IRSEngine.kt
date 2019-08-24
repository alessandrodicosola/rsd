package recommendations.skel

abstract class IRSEngine<in ObjKey : Number, out ItemKey : Number, out RatingType : Number> {
    abstract fun getRecommendations(id: ObjKey): List<RSObject<ItemKey, RatingType>>
}