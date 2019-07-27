package recommendations.concrete

import recommendations.skel.IWeightCalculator
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.math.sqrt
import kotlin.test.assertTrue

/**
 *
 * BOOK: Chapter2. A Comprehensive Survey of Neighborhood-Based Raccomandation Methods
 * AUTHOR: Xia Ning, Christian Desrosiers and George Karypis
 *
 * Calculate weight between two users through Persona Correlation
 * @param ratingsU Rates given by U to items rated also by V
 * @param ratingsV Rates given by V to items rated also by U
 */
class PersonaCorrelation(
    var ratingsU: HashMap<Long, Double>,
    var ratingsV: HashMap<Long, Double>,
    var user: User,
    var neighbor: Neighbor
) :
    IWeightCalculator {
    /**
     * The value calculated is used for setting [neighbor] weight
     * @return Weight between U and V
     */
    override fun calculate(): Double {
        assertTrue(ratingsU.all { ratingsV.containsKey(it.key) })

        val normalizedU = ratingsU.mapValues { it.value - user.avg }
        val normalizedV = ratingsV.mapValues { it.value - neighbor.avg }
        val den1 = normalizedU.mapValues { it.value * it.value }
        val den2 = normalizedV.mapValues { it.value * it.value }
        return normalizedU.mapValues { it.value * normalizedV.get(it.key)!! }.asSequence().sumByDouble { it.value }
            .div(sqrt(den1.asSequence().sumByDouble { it.value }.times(den2.asSequence().sumByDouble { it.value })))
            .let {
                neighbor.weight = it;
                it
            }
    }

}
/**
 * The value calculated is used for setting [neighbor] weight
 * @return Weight between U and V
 */
class WeightedPersonaCorrelation(
    var ratingsU: HashMap<Long, Double>,
    var ratingsV: HashMap<Long, Double>,
    var user: User,
    var neighbor: Neighbor,
    factor: Int
) : WeightedCorrelation(PersonaCorrelation(ratingsU, ratingsV, user, neighbor), ratingsU.size, factor)
