package recommendations.skel

/**
 * Classe che identifica il sistema di raccomandazione
 * @param UserKey Tipo che identifica l'utente
 * @param ItemKey Tipo che identifica l'oggetto
 * @param RatingType Tipo che identifica il rate
 */
abstract class IRSEngine<in UserKey : Number, ItemKey : Number, RatingType : Number> {
    /**
     * Calcola le raccomandazioni
     * @param userId User identification
     * @param itemId Item identification
     */
    abstract fun getRecommendations(userId: UserKey, itemId: ItemKey): List<RSObject<ItemKey, RatingType>>

    /**
     * Aggiorna le informazioni relative a un utente nel database
     * @param userId User identification
     * @param itemId Item identification
     * @param ratingValue Valore del rate dato dall'utente all'oggetto
     */
    abstract fun updateRecommendations(userId: UserKey, itemId: ItemKey, ratingValue: RatingType)
}