package recommendations.concrete

import common.dotProduct
import recommendations.skel.IRatingCalculator
import kotlin.math.pow


/**
 * Calculate the user's rating for specific item
 * @param meanOverall
 * @param biasUser
 * @param biasItem
 * @param factorsUser
 * @param factorsNeighbors
 * @param mapRatingsNeighbors
 * @param mapBiasNeighbors Map of B_vi of neighbors
 */
@Deprecated("Neighborhood_Learning_Engine_2 non è stato inserito nella tesi")
class Latent2_RatingCalculator(
    private val meanOverall: Double,
    private val biasUser: Double,
    private val biasItem: Double,
    private val factorsUser: DoubleArray,
    private val indipendentFromU : DoubleArray
) : IRatingCalculator<Double> {
    override fun calculate(): Double {
        val sum = factorsUser.dotProduct(indipendentFromU)
        return meanOverall + biasUser + biasItem + sum
    }
}