package recommendations.concrete

import recommendations.skel.IRatingCalculator
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.math.absoluteValue
import kotlin.streams.asSequence

/**
 * Calculate ZScore Rating using this formula:
 *
 * >                              SUM(w_uv * (r_vi - <r_v>)
 * >      r_ui = <r_u> + std_u * --------------------------       where v belong to the set of neighbors respects u who rated item i
 * >                                  SUM(|w_uv|)
 *
 */
class ZScoreRating(var user: User, var neighbors: List<Neighbor>, var ratingMap: Map<Long, Double>) :
    IRatingCalculator<Double> {

    override fun calculate(): Double {
        val num =
            neighbors.map { ((ratingMap.get(it.id)!! - it.avg) / it.std) * it.weight }.asSequence()
                .sumByDouble { it }
        val den = neighbors.sumByDouble { it.weight.absoluteValue }
        val rate = num / den
        val rating = user.avg + user.std * rate
        return rating
    }
}