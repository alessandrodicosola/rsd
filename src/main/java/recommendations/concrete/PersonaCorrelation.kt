package recommendations.concrete

import recommendations.skel.IWeightCalculator
import recommendations.skel.Neighbor
import recommendations.skel.RSObject
import recommendations.skel.User
import java.util.function.BiFunction
import kotlin.math.sqrt

/**
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

class WeightedPersonaCorrelation(
    var ratingsU: HashMap<Long, Double>,
    var ratingsV: HashMap<Long, Double>,
    var user: User,
    var neighbor: Neighbor,
    factor: Int
) : WeightedCorrelation(PersonaCorrelation(ratingsU, ratingsV, user, neighbor), ratingsU.size, factor)
