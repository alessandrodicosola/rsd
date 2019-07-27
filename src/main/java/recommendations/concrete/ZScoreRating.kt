package recommendations.concrete

import recommendations.skel.IRatingCalculator
import recommendations.skel.IWeightCalculator
import recommendations.skel.Neighbor
import recommendations.skel.User
import kotlin.streams.asSequence

/*
        Calculate ZScore Rating using this formula:
                                SUM(w_uv * (r_vi - <r_v>)
        r_ui = <r_u> + std_u * --------------------------       where v belong to the set of neighbors user respects u who rated item i
                                    SUM(|w_uv|)
 */
class ZScoreRating(var user: User, var neighbors: List<Neighbor>) :
    IRatingCalculator {

    override fun calculate(): Double {
        val neighbors_clone = neighbors;
        return neighbors_clone.parallelStream().map { ((it.score - it.avg) / it.std) * it.weight }.asSequence().sum()
            .div(neighbors.sumByDouble { it.weight }).times(user.std) + user.std;
    }
}