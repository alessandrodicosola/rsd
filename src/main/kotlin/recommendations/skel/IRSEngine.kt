package recommendations.skel

abstract class IRSEngine<in UserKey : Number, ItemKey : Number, RatingType : Number> {
    abstract fun getRecommendations(userId: UserKey, itemId: ItemKey): List<RSObject<ItemKey, RatingType>>
    abstract fun updateRecommendations(userId: UserKey, itemId: ItemKey, ratingType: RatingType)
}