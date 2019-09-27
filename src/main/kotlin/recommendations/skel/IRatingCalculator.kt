package recommendations.skel

/**
 * Interfaccia per calcolare la preferenze di un utente rispetto a un oggetto
 * @param RatingType Tipo di numero del rate: Byte,Int,Float,Double
 */
interface IRatingCalculator<out RatingType> {
    fun calculate(): RatingType
}