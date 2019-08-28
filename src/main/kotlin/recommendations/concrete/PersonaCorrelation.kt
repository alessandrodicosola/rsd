package recommendations.concrete

import recommendations.skel.IWeightCalculator
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Calculate weight between two users through Persona Correlation
 * @param ratingsU Rates given by U to items rated also by V
 * @param ratingsV Rates given by V to items rated also by U
 */
class PersonaCorrelation<Key>(
    private val ratingsU: Map<Key, Double>,
    private val ratingsV: Map<Key, Double>,
    private val user: User,
    private val neighbor: Neighbor
) :
    IWeightCalculator {
    /**
     * @return Weight between U and V
     */
    override fun calculate(): Double {
        assertEquals(ratingsU.size, ratingsV.size)
        assertTrue(ratingsU.all { ratingsV.containsKey(it.key) }, "ratingsU and ratingsV contains different keys")


        val normalizedU = ratingsU.mapValues { it.value - user.avg }
        val normalizedV = ratingsV.mapValues { it.value - neighbor.avg }

        val num = normalizedU.mapValues { it.value * normalizedV[it.key]!! }.asSequence().sumByDouble { it.value }
        val den1 = normalizedU.mapValues { it.value * it.value }.asSequence().sumByDouble { it.value }
        val den2 = normalizedV.mapValues { it.value * it.value }.asSequence().sumByDouble { it.value }
        val prodDen = sqrt(den1 * den2)

        return num / prodDen
    }

}

/**
 * @return Weight between U and V
 */
class WeightedPersonaCorrelation<Key>(
    ratingsU: Map<Key, Double>,
    ratingsV: Map<Key, Double>,
    user: User,
    neighbor: Neighbor,
    factor: Int
) : WeightedCorrelation(PersonaCorrelation(ratingsU, ratingsV, user, neighbor), ratingsU.size, factor)
