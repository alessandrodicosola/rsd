package recommendations.concrete

import recommendations.skel.IWeightCalculator
import kotlin.math.min

/*
 * Use the number of items, used for calculating the weight, for normalize the weight in order to give more importance to weight obtained with a lot of items
 */
open class WeightedCorrelation(
    private val weightCalculator: IWeightCalculator,
    private val numberOfItems: Int,
    private val factor: Int
) : IWeightCalculator {
    override fun calculate(): Double {
        //Calcolo il peso con un generico IWeightCalculator
        val w = weightCalculator.calculate()
        //Peso la somiglianza
        val f = (min(numberOfItems, factor).toDouble() / factor)
        return f * w
    }
}